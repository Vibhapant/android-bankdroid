/*
 * Copyright (C) 2010 Nullbyte <http://nullbyte.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liato.bankdroid.banking.banks;

import com.liato.bankdroid.Helpers;
import com.liato.bankdroid.banking.Account;
import com.liato.bankdroid.banking.Bank;
import com.liato.bankdroid.banking.Transaction;
import com.liato.bankdroid.banking.exceptions.BankChoiceException;
import com.liato.bankdroid.banking.exceptions.BankException;
import com.liato.bankdroid.banking.exceptions.LoginException;
import com.liato.bankdroid.legacy.R;
import com.liato.bankdroid.provider.IBankTypes;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.nullbyte.android.urllib.CertificateReader;
import eu.nullbyte.android.urllib.Urllib;

public class Villabanken extends Bank {

    private static final String TAG = "Villabanken";

    private static final String NAME = "Villabanken";

    private static final String NAME_SHORT = "villabanken";

    private static final String URL
            = "https://kundportal.cerdo.se/villabankenpub/card/default.aspx";

    private static final int BANKTYPE_ID = IBankTypes.VILLABANKEN;

    private final Pattern reDisposableAmount = Pattern.compile(
            "<[^>]+>((?:Kvar att utnyttja:)+)[^>]+>([^<]+)");

    private final Pattern reBalance = Pattern.compile(
            "<[^>]+>((?:Utnyttjad kredit:)+)[^>]+>([^<]+)");

    private final Pattern reCreditLimit = Pattern.compile(
            "<[^>]+>((?:Beviljad kredit:)+)[^>]+>([^<]+)");

    private final Pattern reTransactions = Pattern.compile(
            "<[^>]+>(\\d{4}-\\d{2}-\\d{2})[^>]+><[^>]*>+([^<]+)<[^>]*><[^>]*>([^<]+) SEK<");

    private final Pattern reRequestDigest = Pattern.compile(
            "__REQUESTDIGEST\".*?value=\"([^\"]+)\"");

    private final Pattern reViewState = Pattern.compile("__VIEWSTATE\".*?value=\"([^\"]+)\"");

    private final Pattern reEventValidation = Pattern.compile(
            "__EVENTVALIDATION\".*?value=\"([^\"]+)\"");

    private final Pattern reCtl00 = Pattern.compile("\"(ctl00.*?ctl00)\"");

    private String accountUrl
            = "https://kundportal.cerdo.se/villabankenpub/card/secure/CardAccountOverview.aspx";

    ;

    private String accountResponse = null;

    public Villabanken(Context context) {
        super(context);
        super.TAG = TAG;
        super.NAME = NAME;
        super.NAME_SHORT = NAME_SHORT;
        super.BANKTYPE_ID = BANKTYPE_ID;
        super.URL = URL;
    }

    public Villabanken(String username, String password, Context context) throws BankException,
            LoginException, BankChoiceException, IOException {
        this(context);
        this.update(username, password);
    }

    @Override
    protected LoginPackage preLogin() throws BankException, IOException {
        urlopen = new Urllib(context,
                CertificateReader.getCertificates(context, R.raw.cert_villabanken));
        String preLoginResponse = urlopen.open(URL);
        Matcher matcher = reRequestDigest.matcher(preLoginResponse);
        if (!matcher.find()) {
            throw new BankException(
                    res.getText(R.string.unable_to_find).toString() + " request digest.");
        }
        String requestDigest = matcher.group(1);

        matcher = reCtl00.matcher(preLoginResponse);
        if (!matcher.find()) {
            throw new BankException(res.getText(R.string.unable_to_find).toString() + " ctl00");
        }
        String ctl00 = matcher.group(1);

        matcher = reViewState.matcher(preLoginResponse);
        if (!matcher.find()) {
            throw new BankException(
                    res.getText(R.string.unable_to_find).toString() + " view state.");
        }
        String viewState = matcher.group(1);

        matcher = reEventValidation.matcher(preLoginResponse);
        if (!matcher.find()) {
            throw new BankException(
                    res.getText(R.string.unable_to_find).toString() + " event validation.");
        }
        String eventValidation = matcher.group(1);

        List<NameValuePair> postData = new ArrayList<NameValuePair>();
        postData.add(new BasicNameValuePair("MSOWebPartPage_PostbackSource", ""));
        postData.add(new BasicNameValuePair("MSOTlPn_SelectedWpId", ""));
        postData.add(new BasicNameValuePair("MSOTlPn_View", "0"));
        postData.add(new BasicNameValuePair("MSOTlPn_ShowSettings", "False"));
        postData.add(new BasicNameValuePair("MSOGallery_SelectedLibrary", ""));
        postData.add(new BasicNameValuePair("MSOGallery_FilterString", ""));
        postData.add(new BasicNameValuePair("MSOTlPn_Button", "none"));
        postData.add(new BasicNameValuePair("__EVENTTARGET", ""));
        postData.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
        postData.add(new BasicNameValuePair("__REQUESTDIGEST", requestDigest));
        postData.add(new BasicNameValuePair("MSOSPWebPartManager_DisplayModeName", "Browse"));
        postData.add(new BasicNameValuePair("MSOSPWebPartManager_ExitingDesignMode", "false"));
        postData.add(new BasicNameValuePair("MSOWebPartPage_Shared", ""));
        postData.add(new BasicNameValuePair("MSOLayout_LayoutChanges", ""));
        postData.add(new BasicNameValuePair("MSOLayout_InDesignMode", ""));
        postData.add(new BasicNameValuePair("_wpSelected", ""));
        postData.add(new BasicNameValuePair("_wzSelected", ""));
        postData.add(new BasicNameValuePair("MSOSPWebPartManager_OldDisplayModeName", "Browse"));
        postData.add(
                new BasicNameValuePair("MSOSPWebPartManager_StartWebPartEditingName", "false"));
        postData.add(new BasicNameValuePair("MSOSPWebPartManager_EndWebPartEditing", "false"));
        postData.add(new BasicNameValuePair("__LASTFOCUS", ""));
        postData.add(new BasicNameValuePair("__VIEWSTATE", viewState));
        postData.add(new BasicNameValuePair("__EVENTVALIDATION", eventValidation));
        postData.add(new BasicNameValuePair(ctl00.replaceAll("ctl00$", "accountNumber"), username));
        postData.add(new BasicNameValuePair(ctl00.replaceAll("ctl00$", "password"), password));
        postData.add(new BasicNameValuePair(ctl00, "Logga in"));
        postData.add(new BasicNameValuePair("__spDummyText1", ""));
        postData.add(new BasicNameValuePair("__spDummyText2", ""));
        postData.add(new BasicNameValuePair("_wpcmWpid", ""));
        postData.add(new BasicNameValuePair("wpcmVal", ""));

        return new LoginPackage(urlopen, postData, preLoginResponse, URL);
    }

    @Override
    public Urllib login() throws LoginException, BankException, IOException {
        LoginPackage lp = preLogin();
        String loginResponse = urlopen.open(lp.getLoginTarget(), lp.getPostData());
        if (loginResponse.contains("misslyckades")) {
            throw new LoginException(res.getText(R.string.invalid_username_password).toString());
        }
        this.accountResponse = urlopen.open(accountUrl);

        return urlopen;
    }

    @Override
    public void update() throws BankException, LoginException, BankChoiceException, IOException {
        super.update();
        if (username == null || password == null || username.length() == 0
                || password.length() == 0) {
            throw new LoginException(res.getText(R.string.invalid_username_password).toString());
        }
        urlopen = login();

        Matcher matcher;

        matcher = reDisposableAmount.matcher(accountResponse);
        if (matcher.find()) {
            Account account = new Account("Disponibelt belopp",
                    Helpers.parseBalance(matcher.group(2)), "0");
            account.setType(Account.CCARD);
            account.setCurrency(currency);
            accounts.add(account);
            balance = balance.add(account.getBalance());
        }
        matcher = reBalance.matcher(accountResponse);
        if (matcher.find()) {
            Account account = new Account("Saldo", Helpers.parseBalance(matcher.group(2)), "1");
            account.setType(Account.OTHER);
            account.setAliasfor("Saldo alias");
            account.setCurrency(currency);
            accounts.add(account);
        }
        matcher = reCreditLimit.matcher(accountResponse);
        if (matcher.find()) {
            Account account = new Account("Köpgräns", Helpers.parseBalance(matcher.group(2)), "2");
            account.setType(Account.OTHER);
            account.setAliasfor("Köpgräns alias");
            account.setCurrency(currency);
            accounts.add(account);
        }
        if (accounts.isEmpty()) {
            throw new BankException(res.getText(R.string.no_accounts_found).toString());
        }

        super.updateComplete();
    }

    @Override
    public void updateTransactions(Account account, Urllib urlopen) throws LoginException,
            BankException, IOException {
        super.updateTransactions(account, urlopen);
        if (account.getType() != Account.CCARD) {
            return;
        }

        ArrayList<Transaction> transactions = new ArrayList<Transaction>();
        Matcher matcher = reTransactions.matcher(accountResponse);
        while (matcher.find()) {
            transactions.add(new Transaction(matcher.group(1), matcher.group(2),
                    Helpers.parseBalance(matcher.group(3)).negate(), account.getCurrency()));
        }
        account.setTransactions(transactions);
    }
}
