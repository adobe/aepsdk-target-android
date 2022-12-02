/* ************************************************************************
•    ADOBE CONFIDENTIAL
•    ________________________________________
•
•    Copyright 2019 Adobe
•    All Rights Reserved.
•
•    NOTICE: All information contained herein is, and remains
•    the property of Adobe and its suppliers, if any. The intellectual
•    and technical concepts contained herein are proprietary to Adobe
•    and its suppliers and are protected by all applicable intellectual
•    property laws, including trade secret and copyright laws.
•    Dissemination of this information or reproduction of this material
•    is strictly forbidden unless prior written permission is obtained
•    from Adobe.
**************************************************************************/

package com.adobe.marketing.mobile.target;

import java.util.Date;
import java.util.TimeZone;

class TargetUtil {
	private static final long MILLISECONDS_PER_SECOND = 1000L;
	private static final double SECONDS_PER_MINUTE = 60;

	/**
	 * Gets the UTC time offset in minutes
	 *
	 * @return UTC time offset in minutes
	 */
	static double getUTCTimeOffsetMinutes() {
		final TimeZone tz = TimeZone.getDefault();
		final Date now = new Date();
		return tz.getOffset(now.getTime()) / (double) MILLISECONDS_PER_SECOND / SECONDS_PER_MINUTE;
	}

	private TargetUtil() {}
}
