/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.poshi.runner.util;

import java.math.BigInteger;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jodd.util.Base32;

/**
 * @author Della Wang
 */
public class AuthenticationUtil {

	public static String generateTOTP(String encodedText) {
		String otpAlgorithm = "HmacSHA1";

		byte[] secret = Base32.decode(encodedText);

		long time = (System.currentTimeMillis() - 3000) / 30000;

		String hex = StringUtil.toUpperCase(Long.toHexString(time));

		if (hex.length() > 16) {
			return hex;
		}

		hex = StringUtil.replace(
			String.format("%16s", hex), CharPool.SPACE, CharPool.NUMBER_0);

		try {
			Mac mac = Mac.getInstance(otpAlgorithm);

			mac.init(new SecretKeySpec(secret, "RAW"));

			BigInteger bigInteger = new BigInteger("10" + hex, 16);

			byte[] byteArray = bigInteger.toByteArray();

			byte[] hash = mac.doFinal(
				Arrays.copyOfRange(byteArray, 1, byteArray.length));

			int offset = hash[hash.length - 1] & 0xf;

			int binary =
				((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16) |
				((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);

			int totp = binary % (int)Math.pow(10, 6);

			return String.format("%0" + 6 + "d", totp);
		}
		catch (InvalidKeyException invalidKeyException) {
			throw new IllegalArgumentException(
				"Invalid secret key for algorithm " + otpAlgorithm, invalidKeyException);

		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new IllegalArgumentException(
				"Invalid algorithm " + otpAlgorithm, noSuchAlgorithmException);
		}
	}
}