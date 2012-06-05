package org.apache.archiva.web.test;

import org.apache.archiva.web.test.tools.SeleniumNetworkCapture;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Antoine ROUAZE <antoine.rouaze AT zenika.com>
 */
public class CUDFTest extends ArchivaAdminTest {

    private static final String SHOW_BUTTON_ID= "show-cudf-universe-button";

    @Test
    public void testCUDFShowCUDF()
    {
        login(getAdminUsername(), getAdminPassword());
        clickLinkWithLocator("menu-cudf-extract-universe", true);
        assertTextPresent("You can get a CUDF file for the entire repository. Ask Archiva to show you the universe.");
        getSelenium().captureNetworkTraffic("plain");
        clickButtonWithLocator(SHOW_BUTTON_ID, true);
        String toto = getSelenium().captureNetworkTraffic("plain");
        System.out.println(toto);
        assertButtonIsDisable(SHOW_BUTTON_ID);
        assertTextPresent("preamble:");
    }

    @Test
    @SeleniumNetworkCapture
    public void testExtractCUDFToFile()
    {
        login(getAdminUsername(), getAdminPassword());
        clickLinkWithLocator("menu-cudf-extract-universe", true);
        assertTextPresent("You can get a CUDF file for the entire repository. Ask Archiva to show you the universe.");
        getSelenium().captureNetworkTraffic("plain");
        clickButtonWithLocator("get-cudf-universe-extract", true);
        assertExtractCUDFIsDownload();
    }

    private void assertExtractCUDFIsDownload()
    {
        String response = getSelenium().captureNetworkTraffic("plain");
        System.out.println(response);
        Assert.assertTrue("The Content-Disposition header isn't present.",response.contains("Content-Disposition => attachment; filename=extractCUDF_universe.txt"));
        Assert.assertTrue("The Content-Type header isn't present or is not valide", response.contains("Content-Type => application/octet-stream;charset=UTF-8"));
    }


    private void assertButtonIsDisable(String locator)
    {
        String styleClass = getSelenium().getAttribute(String.format("%s@class", locator));
        Assert.assertEquals("The Show CUDF button should be disabled after being clicked", "btn disabled", styleClass);
    }


}
