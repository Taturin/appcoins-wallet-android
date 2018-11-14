package com.asfoundation.wallet.ui.iab.raiden;

import com.asf.microraidenj.type.Address;
import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MultiWalletNonceObtainerTest {
  public static final String ADDRESS_1 = "0x4FBcc5cE88493C3D9903701C143aF65F54481119";
  public static final String ADDRESS_2 = "0x4FBcc5cE88493C3D9903701C143aF65F54481112";
  public static final int CHAIN_ID_1 = 1;
  public static final int CHAIN_ID_2 = 2;
  private static int nonce = 0;
  private MultiWalletNonceObtainer nonceObtainer;

  @Before public void setUp() throws Exception {
    nonceObtainer = new MultiWalletNonceObtainer(
        new NonceObtainerFactory(1, address -> new BigInteger(String.valueOf(nonce))));
  }

  @Test public void getNonce() {
    assertEquals("wrong nonce", BigInteger.ZERO,
        nonceObtainer.getNonce(Address.from(ADDRESS_1), CHAIN_ID_1));
    assertEquals("wrong nonce", BigInteger.ZERO,
        nonceObtainer.getNonce(Address.from(ADDRESS_1), CHAIN_ID_1));
  }

  @Test public void consumeNonce() {
    assertEquals("wrong nonce", BigInteger.ZERO,
        nonceObtainer.getNonce(Address.from(ADDRESS_1), CHAIN_ID_1));
    nonceObtainer.consumeNonce(BigInteger.ZERO, Address.from(ADDRESS_1), CHAIN_ID_1);
    assertEquals("wrong nonce", BigInteger.ONE,
        nonceObtainer.getNonce(Address.from(ADDRESS_1), CHAIN_ID_1));
  }

  @Test public void getNonceMultipleWallet() {
    assertEquals("wrong nonce", BigInteger.ZERO,
        nonceObtainer.getNonce(Address.from(ADDRESS_1), CHAIN_ID_1));
    assertEquals("wrong nonce", BigInteger.ZERO,
        nonceObtainer.getNonce(Address.from(ADDRESS_2), CHAIN_ID_1));
  }

  @Test public void consumeNonceMultipleWallet() {
    Address address = Address.from(ADDRESS_1);
    int chainId = CHAIN_ID_1;
    assertEquals("wrong nonce", BigInteger.ZERO, nonceObtainer.getNonce(address, chainId));
    nonceObtainer.consumeNonce(BigInteger.ZERO, address, chainId);

    assertEquals("wrong nonce", BigInteger.ONE, nonceObtainer.getNonce(address, chainId));

    address = Address.from(ADDRESS_2);
    assertEquals("wrong nonce", BigInteger.ZERO, nonceObtainer.getNonce(address, chainId));
    nonceObtainer.consumeNonce(BigInteger.ZERO, address, chainId);
    assertEquals("wrong nonce", BigInteger.ONE, nonceObtainer.getNonce(address, chainId));

    address = Address.from(ADDRESS_1);
    chainId = CHAIN_ID_2;
    assertEquals("wrong nonce", BigInteger.ZERO, nonceObtainer.getNonce(address, chainId));
    nonceObtainer.consumeNonce(BigInteger.ZERO, address, chainId);

    assertEquals("wrong nonce", BigInteger.ONE, nonceObtainer.getNonce(address, chainId));

    address = Address.from(ADDRESS_2);
    assertEquals("wrong nonce", BigInteger.ZERO, nonceObtainer.getNonce(address, chainId));
    nonceObtainer.consumeNonce(BigInteger.ZERO, address, chainId);
    assertEquals("wrong nonce", BigInteger.ONE, nonceObtainer.getNonce(address, chainId));
  }
}