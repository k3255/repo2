/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This code was extracted from the Java cryptography library from
 * www.bouncycastle.org. The code has been formatted to comply with the rest of
 * the formatting in this library.
 */
package com.raonsecure.odi.crypto.ec;

import java.math.BigInteger;

/**
 * This class represents an elliptic curve point.
 */
public class EcPoint {

	private EcCurve _curve;

	private EcFieldElement _x;

	private EcFieldElement _y;

	private boolean _compressed;

	public EcPoint(EcCurve curve, EcFieldElement x, EcFieldElement y) {
		this(curve, x, y, false);
	}

	public EcPoint(EcCurve curve, EcFieldElement x, EcFieldElement y,
			boolean compressed) {
		this._curve = curve;
		this._x = x;
		this._y = y;
		this._compressed = compressed;
	}

	public EcCurve getCurve() {
		return _curve;
	}

	public EcFieldElement getX() {
		return _x;
	}

	public EcFieldElement getY() {
		return _y;
	}

	public boolean isInfinity() {
		return _x == null && _y == null;
	}

	public boolean isCompressed() {
		return _compressed;
	}

	/**
	 * return the field element encoded with point compression. (S 4.3.6)
	 */
	public byte[] getEncoded() {
		if (this.isInfinity()) {
			return new byte[1];
		}

		int length = EcTools.getByteLength(_x.getFieldSize());

		if (_compressed) {
			byte PC;

			if (this.getY().toBigInteger().testBit(0)) {
				PC = 0x03;
			}
			else {
				PC = 0x02;
			}

			byte[] X = EcTools.integerToBytes(this.getX().toBigInteger(), length);
			byte[] PO = new byte[X.length + 1];

			PO[0] = PC;
			System.arraycopy(X, 0, PO, 1, X.length);

			return PO;
		}
		else {
			byte[] X = EcTools.integerToBytes(this.getX().toBigInteger(), length);
			byte[] Y = EcTools.integerToBytes(this.getY().toBigInteger(), length);
			byte[] PO = new byte[X.length + Y.length + 1];

			PO[0] = 0x04;
			System.arraycopy(X, 0, PO, 1, X.length);
			System.arraycopy(Y, 0, PO, X.length + 1, Y.length);

			return PO;
		}
	}

	// B.3 pg 62
	public EcPoint add(EcPoint b) {
		if (this.isInfinity()) {
			return b;
		}

		if (b.isInfinity()) {
			return this;
		}

		// Check if b = this or b = -this
		if (this._x.equals(b._x)) {
			if (this._y.equals(b._y)) {
				// this = b, i.e. this must be doubled
				return this.twice();
			}

			// this = -b, i.e. the result is the point at infinity
			return this._curve.getInfinity();
		}

		EcFieldElement gamma = b._y.subtract(this._y).divide(b._x.subtract(this._x));

		EcFieldElement x3 = gamma.square().subtract(this._x).subtract(b._x);
		EcFieldElement y3 = gamma.multiply(this._x.subtract(x3)).subtract(this._y);

		return new EcPoint(_curve, x3, y3);
	}

	// B.3 pg 62
	public EcPoint twice() {
		if (this.isInfinity()) {
			// Twice identity element (point at infinity) is identity
			return this;
		}

		if (this._y.toBigInteger().signum() == 0) {
			// if y1 == 0, then (x1, y1) == (x1, -y1)
			// and hence this = -this and thus 2(x1, y1) == infinity
			return this._curve.getInfinity();
		}

		EcFieldElement TWO = this._curve.fromBigInteger(BigInteger.valueOf(2));
		EcFieldElement THREE = this._curve.fromBigInteger(BigInteger.valueOf(3));
		EcFieldElement gamma = this._x.square().multiply(THREE).add(_curve.getA())
				.divide(_y.multiply(TWO));

		EcFieldElement x3 = gamma.square().subtract(this._x.multiply(TWO));
		EcFieldElement y3 = gamma.multiply(this._x.subtract(x3)).subtract(this._y);

		return new EcPoint(_curve, x3, y3, this._compressed);
	}

	// D.3.2 pg 102 (see Note:)
	public EcPoint subtract(EcPoint b) {
		if (b.isInfinity()) {
			return this;
		}

		// Add -b
		return add(b.negate());
	}

	public EcPoint negate() {
		return new EcPoint(_curve, this._x, this._y.negate(), this._compressed);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (!(other instanceof EcPoint)) {
			return false;
		}

		EcPoint o = (EcPoint) other;

		if (this.isInfinity()) {
			return o.isInfinity();
		}

		return _x.equals(o._x) && _y.equals(o._y);
	}

	@Override
	public int hashCode() {
		if (this.isInfinity()) {
			return 0;
		}

		return _x.hashCode() ^ _y.hashCode();
	}

	public EcPoint multiply(BigInteger n) {
		return EcTools.multiply(this, n);
	}

}
