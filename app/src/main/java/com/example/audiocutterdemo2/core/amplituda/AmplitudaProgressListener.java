package com.example.audiocutterdemo2.core.amplituda;

public abstract class AmplitudaProgressListener {

    private ProgressOperation operation;

    void onOperationChanged(ProgressOperation operation) {
        this.operation = operation;
    }

    void onProgressInternal(String path, int progress, String data, int totalFrame) {
        onProgress(operation, path, progress, data, totalFrame);
    }

    /**
     * Public API
     */

    public void onStartProgress() {
    }

    public void onStopProgress() {
    }

    public abstract void onProgress(ProgressOperation operation, String path, int progress, String data, int totalFrame);

}
