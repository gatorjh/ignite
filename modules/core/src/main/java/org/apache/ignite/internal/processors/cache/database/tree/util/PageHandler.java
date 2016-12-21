/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.database.tree.util;

import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.pagemem.Page;
import org.apache.ignite.internal.pagemem.wal.IgniteWriteAheadLogManager;
import org.apache.ignite.internal.pagemem.wal.record.delta.InitNewPageRecord;
import org.apache.ignite.internal.processors.cache.database.tree.io.PageIO;
import org.apache.ignite.internal.util.GridUnsafe;
import sun.nio.ch.DirectBuffer;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Page handler.
 */
public abstract class PageHandler<X, R> {
    /** */
    private static final PageHandler<Void, Boolean> NOOP = new PageHandler<Void, Boolean>() {
        @Override public Boolean run(Page page, PageIO io, long buf, Void arg, int intArg)
            throws IgniteCheckedException {
            return TRUE;
        }
    };

    /**
     * @param page Page.
     * @param io IO.
     * @param buf Page buffer.
     * @param arg Argument.
     * @param intArg Argument of type {@code int}.
     * @return Result.
     * @throws IgniteCheckedException If failed.
     */
    public abstract R run(Page page, PageIO io, long buf, X arg, int intArg)
        throws IgniteCheckedException;

    /**
     * @param page Page.
     * @param arg Argument.
     * @param intArg Argument of type {@code int}.
     * @return {@code true} If release.
     */
    public boolean releaseAfterWrite(Page page, X arg, int intArg) {
        return true;
    }

    /**
     * @param page Page.
     * @param h Handler.
     * @param arg Argument.
     * @param intArg Argument of type {@code int}.
     * @param lockFailed Result in case of lock failure due to page recycling.
     * @return Handler result.
     * @throws IgniteCheckedException If failed.
     */
    public static <X, R> R readPage(
        Page page,
        PageLockListener lockLsnr,
        PageHandler<X, R> h,
        X arg,
        int intArg,
        R lockFailed
    ) throws IgniteCheckedException {
        long buf = readLock(page, lockLsnr);

        if (buf == 0L)
            return lockFailed;

        try {
            PageIO io = PageIO.getPageIO(buf);

            return h.run(page, io, buf, arg, intArg);
        }
        finally {
            readUnlock(page, buf, lockLsnr);
        }
    }

    /**
     * @param page Page.
     * @param h Handler.
     * @param arg Argument.
     * @param intArg Argument of type {@code int}.
     * @param lockFailed Result in case of lock failure due to page recycling.
     * @return Handler result.
     * @throws IgniteCheckedException If failed.
     */
    public static <X, R> R writePage(
        Page page,
        PageLockListener lockLsnr,
        PageHandler<X, R> h,
        X arg,
        int intArg,
        R lockFailed
    ) throws IgniteCheckedException {
        return writePage(page, lockLsnr, h, null, null, arg, intArg, lockFailed);
    }

    /**
     * @param page Page.
     * @param lockLsnr Lock listener.
     * @param init IO for new page initialization or {@code null} if it is an existing page.
     * @throws IgniteCheckedException If failed.
     */
    public static void initPage(
        Page page,
        PageLockListener lockLsnr,
        PageIO init,
        IgniteWriteAheadLogManager wal
    ) throws IgniteCheckedException {
        Boolean res = writePage(page, lockLsnr, NOOP, init, wal, null, 0, FALSE);

        assert res == TRUE : res; // It must be newly allocated page, can't be recycled.
    }

    /**
     * @param page Page.
     * @param lockLsnr Lock listener.
     * @return Byte buffer or {@code null} if failed to lock due to recycling.
     */
    public static long readLock(Page page, PageLockListener lockLsnr) {
        lockLsnr.onBeforeReadLock(page);

        long buf = page.getForReadPointer();

        lockLsnr.onReadLock(page, buf);

        return buf;
    }

    /**
     * @param page Page.
     * @param buf Page buffer.
     * @param lockLsnr Lock listener.
     */
    public static void readUnlock(Page page, long buf, PageLockListener lockLsnr) {
        lockLsnr.onReadUnlock(page, buf);

        page.releaseRead();
    }

    /**
     * @param page Page.
     * @param lockLsnr Lock listener.
     * @param tryLock Only try to lock without waiting.
     * @return Byte buffer or {@code null} if failed to lock due to recycling.
     */
    public static long writeLock(Page page, PageLockListener lockLsnr, boolean tryLock) {
        lockLsnr.onBeforeWriteLock(page);

        long buf = tryLock ? page.tryGetForWritePointer() : page.getForWritePointer();

        lockLsnr.onWriteLock(page, buf);

        return buf;
    }

    /**
     * @param page Page.
     * @param buf Page buffer.
     * @param lockLsnr Lock listener.
     * @param dirty Page is dirty.
     */
    public static void writeUnlock(Page page, long buf, PageLockListener lockLsnr, boolean dirty) {
        lockLsnr.onWriteUnlock(page, buf);

        page.releaseWrite(dirty);
    }

    /**
     * @param page Page.
     * @param lockLsnr Lock listener.
     * @param h Handler.
     * @param init IO for new page initialization or {@code null} if it is an existing page.
     * @param arg Argument.
     * @param intArg Argument of type {@code int}.
     * @param lockFailed Result in case of lock failure due to page recycling.
     * @return Handler result.
     * @throws IgniteCheckedException If failed.
     */
    public static <X, R> R writePage(
        Page page,
        PageLockListener lockLsnr,
        PageHandler<X, R> h,
        PageIO init,
        IgniteWriteAheadLogManager wal,
        X arg,
        int intArg,
        R lockFailed
    ) throws IgniteCheckedException {
        long buf = writeLock(page, lockLsnr, false);

        if (buf == 0L)
            return lockFailed;

        R res;

        boolean ok = false;

        try {
            if (init != null) // It is a new page and we have to initialize it.
                doInitPage(page, buf, init, wal);
            else
                init = PageIO.getPageIO(buf);

            res = h.run(page, init, buf, arg, intArg);

            ok = true;
        }
        finally {
            assert PageIO.getCrc(buf) == 0; //TODO GG-11480

            if (h.releaseAfterWrite(page, arg, intArg))
                writeUnlock(page, buf, lockLsnr, ok);
        }

        return res;
    }

    /**
     * @param page Page.
     * @param buf Buffer.
     * @param init Initial IO.
     * @param wal Write ahead log.
     * @throws IgniteCheckedException If failed.
     */
    private static void doInitPage(
        Page page,
        long buf,
        PageIO init,
        IgniteWriteAheadLogManager wal
    ) throws IgniteCheckedException {
        assert PageIO.getCrc(buf) == 0; //TODO GG-11480

        long pageId = page.id();

        init.initNewPage(buf, pageId);

        // Here we should never write full page, because it is known to be new.
        page.fullPageWalRecordPolicy(FALSE);

        if (isWalDeltaRecordNeeded(wal, page))
            wal.log(new InitNewPageRecord(page.fullId().cacheId(), page.id(),
                init.getType(), init.getVersion(), pageId));
    }

    /**
     * @param wal Write ahead log.
     * @param page Page.
     * @return {@code true} If we need to make a delta WAL record for the change in this page.
     */
    public static boolean isWalDeltaRecordNeeded(IgniteWriteAheadLogManager wal, Page page) {
        // If the page is clean, then it is either newly allocated or just after checkpoint.
        // In both cases we have to write full page contents to WAL.
        return wal != null && !wal.isAlwaysWriteFullPages() && page.fullPageWalRecordPolicy() != TRUE &&
            (page.fullPageWalRecordPolicy() == FALSE || page.isDirty());
    }

    /**
     * @param src Source.
     * @param dst Destination.
     * @param srcOff Source offset in bytes.
     * @param dstOff Destination offset in bytes.
     * @param cnt Bytes count to copy.
     */
    public static void copyMemory(ByteBuffer src, ByteBuffer dst, long srcOff, long dstOff, long cnt) {
        byte[] srcArr = src.hasArray() ? src.array() : null;
        byte[] dstArr = dst.hasArray() ? dst.array() : null;
        long srcArrOff = src.hasArray() ? src.arrayOffset() + GridUnsafe.BYTE_ARR_OFF : 0;
        long dstArrOff = dst.hasArray() ? dst.arrayOffset() + GridUnsafe.BYTE_ARR_OFF : 0;

        long srcPtr = src.isDirect() ? ((DirectBuffer)src).address() : 0;
        long dstPtr = dst.isDirect() ? ((DirectBuffer)dst).address() : 0;

        GridUnsafe.copyMemory(srcArr, srcPtr + srcArrOff + srcOff, dstArr, dstPtr + dstArrOff + dstOff, cnt);
    }

    public static void copyMemory(long src, long dst, long srcOff, long dstOff, long cnt) {
        GridUnsafe.copyMemory(null, src + srcOff, null, dst + dstOff, cnt);
    }

    public static void zeroMemory(long buf, int off, int len) {
        GridUnsafe.setMemory(buf + off, len, (byte)0);
    }
}
