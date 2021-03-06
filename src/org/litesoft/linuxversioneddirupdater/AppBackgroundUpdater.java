// This Source Code is in the Public Domain per: http://unlicense.org
package org.litesoft.linuxversioneddirupdater;

/**
 * Callback interfaces for Updater progress.
 */
public abstract class AppBackgroundUpdater {
    private final Updater mUpdater;
    private final Callback mCallback;

    private int mExpectedTargets;
    private int mStarted;
    private int[] mOutComeCounts = new int[Outcome.values().length];
    private boolean mRunAgain;

    public AppBackgroundUpdater( Updater pUpdater, boolean pAsDaemon ) {
        mUpdater = pUpdater;
        mCallback = new InnerCallback();
        Thread zThread = new Thread( new Runnable() {
            @Override
            public void run() {
                do {
                    mRunAgain = false;
                    mUpdater.run( false, mCallback );
                }
                while ( mRunAgain );
            }
        } );
        zThread.setDaemon( pAsDaemon );
        zThread.start();
    }

    public State getState() {
        return mUpdater.getState();
    }

    abstract protected void statsReady();

    protected void runAgain() {
        mRunAgain = true;
    }

    protected boolean areCriticalUpdates() {
        return 0 != mOutComeCounts[Outcome.CriticalUpdate.ordinal()];
    }

    protected boolean areNonCriticalUpdates() {
        return 0 != mOutComeCounts[Outcome.Updated.ordinal()];
    }

    protected boolean areFailed() {
        return 0 != mOutComeCounts[Outcome.Failed.ordinal()];
    }

    protected boolean areUnfinished() {
        return mStarted > completed();
    }

    protected boolean areNotStarted() {
        return mExpectedTargets > mStarted;
    }

    private int completed() {
        int zTotal = 0;
        for ( int zCount : mOutComeCounts ) {
            zTotal += zCount;
        }
        return zTotal;
    }

    private void record( Outcome pOutcome ) {
        mOutComeCounts[pOutcome.ordinal()]++;
    }

    private class InnerCallback implements Callback {
        @Override
        public void starting( int pTargets ) {
            mExpectedTargets = pTargets;
            mStarted = 0;
            mOutComeCounts = new int[Outcome.values().length];
        }

        @Override
        public Target start( String pTarget, String pLocalVersion ) {
            mStarted++;
            return new InnerTargetCallback();
        }

        @Override
        public void finished() {
            statsReady();
        }
    }

    private class InnerTargetCallback implements Callback.Target {
        @Override
        public void completeWithCriticalUpdate( String pPendingUpdatedVersion ) {
            record( Outcome.CriticalUpdate );
        }

        @Override
        public void completeWithNonCriticalUpdate( String pPendingUpdatedVersion ) {
            record( Outcome.Updated );
        }

        @Override
        public void completeNoUpdate() {
            record( Outcome.NoUpdate );
        }

        @Override
        public void fail( String pMessage ) {
            record( Outcome.Failed );
        }
    }
}
