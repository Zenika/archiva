package org.apache.archiva.cudf.report;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@Service
public class CUDFReportGenerator
{


    public File generateReport(File cudfFile) {
        try
        {
            checkDistCheck();
            File reportFile = new File( cudfFile.getParent(), cudfFile.getName() + ".rep" );
            if (reportFile.exists()) {
                return reportFile;
            }
            Process process = Runtime.getRuntime().exec( buildCommand(cudfFile, reportFile), new String[]{}, cudfFile.getParentFile() );
            process.waitFor();
            return reportFile;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to launch Distchek command, check if the tool is installed.", e );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( "Thread or process errors", e );
        }
    }

    private String buildCommand( File cudfFile, File reportFile )
    {
        return new StringBuilder(  )
            .append( "distcheck -tcudf --summary --progress ")
            .append( cudfFile.getName() )
            .append( " > " )
            .append( reportFile.getName() ).toString();
    }

    private void checkDistCheck()
        throws IOException, InterruptedException
    {
        Process process = Runtime.getRuntime().exec( "distcheck --version" );
        BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
        if (process.waitFor() == 0) {
            String line = reader.readLine();
            reader.close();
            if (!"Build version 3.1.4".equals( line )) {
                throw new IllegalStateException( "Unable to use Distcheck: distchek --version return :" + line);
            }
        }
    }

}
