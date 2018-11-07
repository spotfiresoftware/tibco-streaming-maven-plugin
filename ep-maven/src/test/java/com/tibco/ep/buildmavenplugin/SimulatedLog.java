/*******************************************************************************
 * Copyright (C) 2018, TIBCO Software Inc.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.tibco.ep.buildmavenplugin;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulated maven logger to catch the actual output
 * 
 */
public class SimulatedLog implements Log {
    private Logger logger = LoggerFactory.getLogger(SimulatedLog.class);

    private final boolean verbose;
    private String debugText;
    private String infoText;
    private String errorText;
    private String warnText;

    /**
     * Constructor
     * @param verbose true to log all output
     */
    SimulatedLog(boolean verbose) {
        this.verbose = verbose;
        reset();
    }
    
    /**
     * Reset stored stings
     */
    public void reset() {
        debugText = "";
        infoText = "";
        errorText = "";
        warnText = "";
    }
    
    /**
     * Get stored debug log
     * 
     * @return debug log
     */
    public String getDebugLog() {
        return debugText;
    }

    /**
     * Get stored info log
     * 
     * @return info log
     */
    public String getInfoLog() {
        return infoText;
    }
    
    /**
     * Get stored error log
     * 
     * @return error log
     */
    public String getErrorLog() {
        return errorText;
    }

    /**
     * Get stored warn log
     * 
     * @return warn log
     */
    public String getWarnLog() {
        return warnText;
    }
    
    @Override
    public void debug(CharSequence content) {
        if (verbose) {
            logger.info("DEBUG: "+content);
        }
        this.debugText+=content+"\n";
    }

    @Override
    public void debug(Throwable error) {
        if (verbose) {
            logger.info("DEBUG: "+error);
        }
        this.debugText+=error+"\n";
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (verbose) {
            logger.info("DEBUG: "+content+error);
        }
        this.debugText+=content+"\n";
        this.debugText+=error+"\n";
    }

    @Override
    public void error(CharSequence content) { 
        if (verbose) {
            logger.info("ERROR: "+content);
        }
        this.errorText+=content+"\n";
    }

    @Override
    public void error(Throwable error) {
        if (verbose) {
            logger.info("ERROR: "+error);
        }
        this.errorText+=error+"\n";
    }

    @Override
    public void error(CharSequence content, Throwable error) { 
        if (verbose) {
            logger.info("ERROR: "+content+error);
        }
        this.errorText+=content+"\n";
        this.errorText+=error+"\n";
    }

    @Override
    public void info(CharSequence content) {
        if (verbose) {
            logger.info("INFO: "+content);
        }
        this.infoText+=content+"\n";

    }

    @Override
    public void info(Throwable error) {
        if (verbose) {
            logger.info("INFO: "+error);
        }
        this.infoText+=error+"\n";
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (verbose) {
            logger.info("INFO: "+content+error);
        }
        this.infoText+=content+"\n";
        this.infoText+=error+"\n";
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(CharSequence content) {
        if (verbose) {
            logger.info("WARN: "+content);
        }
        this.warnText+=content+"\n";

    }

    @Override
    public void warn(Throwable error) {
        if (verbose) {
            logger.info("WARN: "+error);
        }
        this.warnText+=error+"\n";

    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (verbose) {
            logger.info("WARN: "+content+error);
        }
        this.warnText+=content+"\n";
        this.warnText+=error+"\n";
    }

}
