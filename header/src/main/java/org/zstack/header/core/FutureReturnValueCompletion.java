package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.concurrent.TimeUnit;

/**
 */
public class FutureReturnValueCompletion extends ReturnValueCompletion {
    private volatile boolean success;
    private ErrorCode errorCode;
    private volatile boolean done;
    private Object result;

    public FutureReturnValueCompletion(AsyncBackup... backups) {
        super(backups);
    }

    public FutureReturnValueCompletion() {
        super();
    }

    @Override
    public final synchronized void success(Object returnValue) {
        this.success = true;
        done = true;
        result = returnValue;
        notifyAll();
    }

    @Override
    public synchronized void fail(ErrorCode errorCode) {
        this.success = false;
        this.errorCode = errorCode;
        done = true;
        notifyAll();
    }

    public boolean isSuccess() {
        return success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public synchronized void await(long timeout) {
        if (done) {
            return;
        }

        try {
            wait(timeout);
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }

        if (!done) {
            ErrorCode err = new ErrorCode();
            err.setCode(SysErrors.TIMEOUT.toString());
            err.setDetails(String.format("FutureCompletion timeout after %s seconds", TimeUnit.MILLISECONDS.toSeconds(timeout)));
            fail(err);
        }
    }

    public synchronized void await() {
        if (done) {
            return;
        }

        try {
            wait();
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public <T> T getResult() {
        return (T) result;
    }
}
