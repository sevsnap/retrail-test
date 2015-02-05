/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author oneadmin
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({it.cnr.iit.retrail.test.DALTest.class,it.cnr.iit.retrail.test.SimpleTest.class,it.cnr.iit.retrail.test.MainTest.class,it.cnr.iit.retrail.test.PIPTest.class,it.cnr.iit.retrail.test.PIPAttributesTest.class})
public class ClientSuite {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }
    
}
