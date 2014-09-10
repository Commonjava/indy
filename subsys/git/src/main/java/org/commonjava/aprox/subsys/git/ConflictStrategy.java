package org.commonjava.aprox.subsys.git;

import org.eclipse.jgit.merge.MergeStrategy;

public enum ConflictStrategy
{

    keep( MergeStrategy.OURS ), overwrite( MergeStrategy.THEIRS ), merge( MergeStrategy.RECURSIVE );

    private MergeStrategy mergeStrategy;

    private ConflictStrategy( final MergeStrategy mergeStrategy )
    {
        this.mergeStrategy = mergeStrategy;
    }

    public MergeStrategy mergeStrategy()
    {
        return mergeStrategy;
    }

}
