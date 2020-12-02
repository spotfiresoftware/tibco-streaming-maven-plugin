//
// Copyright (c) 2009-2020 TIBCO Software Inc. All rights reserved.
//
package com.tibco.ep.sample;

/**
 * Demonstrates varargs custom functions
 */
public class VarArgs {

	/**
	 * @param args values to sum
	 * @return the sum of all values given
	 */
	public static double sumAll(double... args) {
		double ret = 0;
		for (double val : args) {
			ret += val;
		}
		return ret;
	}

	/**
	 * @param needle the string to search in haystack
	 * @param haystack the list of strings to find needle in
	 * @return whether needle was in the haystack
	 */
	public static boolean isIn(String needle, String... haystack) {
		for (String candidate : haystack) {
			if (candidate == null) {
				if (needle == null) {
					return true;
				}
			} else {
				if (candidate.contains(needle)) {
					return true;
				}
			}
		}
		return false;
	}

}
