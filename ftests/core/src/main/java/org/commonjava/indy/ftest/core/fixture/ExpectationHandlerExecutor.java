package org.commonjava.indy.ftest.core.fixture;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ExpectationHandlerExecutor
{
    public void execute( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException;
}
