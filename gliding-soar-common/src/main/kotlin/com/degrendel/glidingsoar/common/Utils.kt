package com.degrendel.glidingsoar.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Version
{
  val VERSION: String = if (javaClass.`package`.implementationVersion != null)
    javaClass.`package`.implementationVersion
  else
    "DEVELOPMENT"
}

fun <R : Any> R.logger(): Lazy<Logger> = lazy { LoggerFactory.getLogger(this::class.java) }