/*
 * Copyright 2010 Srikanth Reddy Lingala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lingala.zip4j.crypto;

import net.lingala.zip4j.crypto.engine.ZipCryptoEngine;
import net.lingala.zip4j.exception.ZipException;

import java.util.Random;

import static net.lingala.zip4j.util.InternalZipConstants.STD_DEC_HDR_SIZE;

public class StandardEncrypter implements Encrypter {

  private ZipCryptoEngine zipCryptoEngine;
  private byte[] headerBytes;

  public StandardEncrypter(char[] password, long key) throws ZipException {
   this.zipCryptoEngine = new ZipCryptoEngine();

    this.headerBytes = new byte[STD_DEC_HDR_SIZE];
    init(password, key);
  }

  private void init(char[] password, long key) throws ZipException {
    if (password == null || password.length <= 0) {
      throw new ZipException("input password is null or empty, cannot initialize standard encrypter");
    }
    zipCryptoEngine.initKeys(password);
    headerBytes = generateRandomBytes(STD_DEC_HDR_SIZE);
    // Initialize again since the generated bytes were encrypted.
    zipCryptoEngine.initKeys(password);

    headerBytes[STD_DEC_HDR_SIZE - 1] = (byte) ((key >>> 24));
    headerBytes[STD_DEC_HDR_SIZE - 2] = (byte) ((key >>> 16));

    if (headerBytes.length < STD_DEC_HDR_SIZE) {
      throw new ZipException("invalid header bytes generated, cannot perform standard encryption");
    }

    encryptData(headerBytes);
  }

  public int encryptData(byte[] buff) throws ZipException {
    if (buff == null) {
      throw new NullPointerException();
    }
    return encryptData(buff, 0, buff.length);
  }

  public int encryptData(byte[] buff, int start, int len) throws ZipException {
    if (len < 0) {
      throw new ZipException("invalid length specified to decrpyt data");
    }

    for (int i = start; i < start + len; i++) {
      buff[i] = encryptByte(buff[i]);
    }
    return len;
  }

  protected byte encryptByte(byte val) {
    byte temp_val = (byte) (val ^ zipCryptoEngine.decryptByte() & 0xff);
    zipCryptoEngine.updateKeys(val);
    return temp_val;
  }

  protected byte[] generateRandomBytes(int size) throws ZipException {

    if (size <= 0) {
      throw new ZipException("size is either 0 or less than 0, cannot generate header for standard encryptor");
    }

    byte[] buff = new byte[size];

    Random rand = new Random();

    for (int i = 0; i < buff.length; i++) {
      // Encrypted to get less predictability for poorly implemented rand functions.
      buff[i] = encryptByte((byte) rand.nextInt(256));
    }
    return buff;
  }

  public byte[] getHeaderBytes() {
    return headerBytes;
  }

}
