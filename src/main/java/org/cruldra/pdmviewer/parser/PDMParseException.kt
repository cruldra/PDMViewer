package org.cruldra.pdmviewer.parser

class PDMParseException(private val msg: String, override val cause: Throwable? = null) :
    Exception(msg, cause) {

}
