/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.AuthTokenStorage;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_AUTH_TOKEN;
import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_TOKEN_HISTORY;
import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.TOKEN_HISTORY_LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreferenceTokenStorageTest {

	private Context mContext;

	@Before
	public void setUp() {
		mContext = InstrumentationRegistry.getTargetContext();
		SharedPreferencesManager.initialize(mContext);
	}

	@After
	public void tearDown() {
		SharedPreferencesManager.clear();
	}

	@Test
	public void testPreferenceTokenStorage() {

		/* Mock token. */
		AuthTokenStorage tokenStorage = TokenStorageFactory.getTokenStorage(mContext);
		String mockToken = UUIDUtils.randomUUID().toString();
		String mockAccountId = UUIDUtils.randomUUID().toString();

		/* Save the token into storage. */
		tokenStorage.saveToken(mockToken, mockAccountId, any(Date.class));

		/* Assert that storage returns the same token.*/
		assertEquals(mockToken, tokenStorage.getToken());
		assertEquals(mockAccountId, tokenStorage.getHomeAccountId());

		/* Remove the token from storage. */
		tokenStorage.saveToken(null, null, null);

		/* Assert that there's no token in storage. */
		assertNull(tokenStorage.getToken());
		assertNull(tokenStorage.getHomeAccountId());
	}

	@Test
	public void saveToken() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);

		tokenStorage.saveToken(null, null, null);

		assertEquals(2, tokenStorage.loadTokenHistory().size());


	}

	@Test
	public void tokenHistoryLimit() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);

		for (int i = 0; i < TOKEN_HISTORY_LIMIT + 3; i++) {
			String mockToken = UUIDUtils.randomUUID().toString();
			String mockAccountId = UUIDUtils.randomUUID().toString();
			tokenStorage.saveToken(mockToken, mockAccountId, any(Date.class));
		}

		assertEquals(TOKEN_HISTORY_LIMIT, tokenStorage.loadTokenHistory().size());
	}

	@Test
	public void removeTokenFromHistory() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);


		String mockToken = UUIDUtils.randomUUID().toString();
		String mockAccountId = UUIDUtils.randomUUID().toString();
		tokenStorage.saveToken(mockToken, mockAccountId, any(Date.class));
		assertEquals(2, tokenStorage.loadTokenHistory().size());

		tokenStorage.removeToken(null);
		assertEquals(1, tokenStorage.loadTokenHistory().size());

		SharedPreferencesManager.clear();

		tokenStorage.removeToken(null);
		assertNull(tokenStorage.loadTokenHistory());

		tokenStorage.saveToken(mockToken, mockAccountId, any(Date.class));
	}

	@Test
	public void loadTokenHistory() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
		assertNull(tokenStorage.loadTokenHistory());
		String mockToken = UUIDUtils.randomUUID().toString();
		String mockAccountId = UUIDUtils.randomUUID().toString();
		tokenStorage.saveToken(mockToken, mockAccountId, any(Date.class));
		assertEquals(2, tokenStorage.loadTokenHistory().size());

		SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, "some bad json");
		assertEquals(0, tokenStorage.loadTokenHistory().size());
	}

	@Test
	public void getOldestToken() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
		assertNull(tokenStorage.getOldestToken().getEndTime());
		for (int i = 0; i < 10; i++) {
			String mockToken = UUIDUtils.randomUUID().toString();
			String mockAccountId = UUIDUtils.randomUUID().toString();
			tokenStorage.saveToken(mockToken, mockAccountId, any(Date.class));
		}
		assertNotNull(tokenStorage.getOldestToken());
		assertNotNull(tokenStorage.getOldestToken().getEndTime());
		tokenStorage.saveTokenHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>());
		assertNotNull(tokenStorage.getOldestToken());
		assertNull(tokenStorage.getOldestToken().getEndTime());
	}

	@Test
	public void removeTokenWithTokenNotNull(){
		CryptoUtils cryptoUtils = CryptoUtils.getInstance(mContext);
		when(CryptoUtils.getInstance(mContext)).thenReturn(cryptoUtils);
		when(cryptoUtils.encrypt(anyString())).thenReturn("token-cript");

		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
		assertNull(tokenStorage.loadTokenHistory());
		String mockToken = UUIDUtils.randomUUID().toString();
		String mockAccountId = UUIDUtils.randomUUID().toString();
		tokenStorage.saveToken(mockToken, mockAccountId, any(Date.class));
		assertEquals(2, tokenStorage.loadTokenHistory().size());
	}

    @Test
	public void loadTokenHistoryTestJsonParseException() throws JsonParseException {
        PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
        assertNull(tokenStorage.loadTokenHistory());
        String mockToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        tokenStorage.saveToken(mockToken, mockAccountId, new Date());
        assertEquals(2, tokenStorage.loadTokenHistory().size());

		SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, "some bad json");
        List<PreferenceTokenStorage.TokenStoreEntity> list = tokenStorage.loadTokenHistory();
		assertEquals(0, ((List) list).size());
	}
}
