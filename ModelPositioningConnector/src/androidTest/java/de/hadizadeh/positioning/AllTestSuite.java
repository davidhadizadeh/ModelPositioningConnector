package de.hadizadeh.positioning;

import android.test.suitebuilder.TestSuiteBuilder;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestSuite extends TestSuite {
    public static Test suite() {
        return new TestSuiteBuilder(AllTestSuite.class)
                .includeAllPackagesUnderHere().build();
    }
}
