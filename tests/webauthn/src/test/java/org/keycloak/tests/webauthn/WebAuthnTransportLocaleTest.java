/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.webauthn;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.tests.webauthn.page.WebAuthnLoginPage;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT;
import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_BLE;
import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_INTERNAL;
import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_NFC;
import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_USB;
import static org.keycloak.testsuite.util.FlowUtil.inCurrentRealm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for checking localization for authenticator transport media name
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@KeycloakIntegrationTest
public class WebAuthnTransportLocaleTest extends AbstractWebAuthnVirtualTest {

    @InjectRealm(ref = "webauthn")
    ManagedRealm realm;

    @InjectRunOnServer(realmRef = "webauthn")
    RunOnServerClient runOnServer;

    @Test
    public void localizationTransportUSB() {
        assertLocalizationIndividual(DEFAULT_USB.getOptions(), "USB", "USB");
    }

    @Test
    public void localizationTransportNFC() {
        assertLocalizationIndividual(DEFAULT_NFC.getOptions(), "NFC", "NFC");
    }

    @Test
    public void localizationTransportBluetooth() {
        assertLocalizationIndividual(DEFAULT_BLE.getOptions(), "Bluetooth", "Bluetooth");
    }

    @Test
    public void localizationTransportInternal() {
        assertLocalizationIndividual(DEFAULT_INTERNAL.getOptions(), "Internal", "Interní");
    }

    @Test
    public void multipleTransports() {
        final String AUTHENTICATOR_LABEL = "authenticator#";
        final Integer EXPECTED_COUNT = 5;

        final BiConsumer<DefaultVirtualAuthOptions, Integer> addAndVerifyAuthenticator = (options, number) -> {
            getVirtualAuthManager().useAuthenticator(options.getOptions());
            addWebAuthnCredential(AUTHENTICATOR_LABEL + number);

            int webAuthnCount = getUserCredentialsCount();
            assertThat(webAuthnCount, is(number));
        };

        addAndVerifyAuthenticator.accept(DEFAULT_INTERNAL, 1);
        addAndVerifyAuthenticator.accept(DEFAULT_BLE, 2);
        addAndVerifyAuthenticator.accept(DEFAULT_NFC, 3);
        addAndVerifyAuthenticator.accept(DEFAULT_USB, 4);
        addAndVerifyAuthenticator.accept(DEFAULT, 5);

        setUpWebAuthnFlow("webAuthnFlow");

        logout();
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();
        webAuthnLoginPage.assertCurrent();

        final Supplier<List<WebAuthnLoginPage.WebAuthnAuthenticatorItem>> getItems = () -> {
            final List<WebAuthnLoginPage.WebAuthnAuthenticatorItem> list = webAuthnLoginPage.getItems();
            assertThat(list, notNullValue());
            assertThat(list.size(), is(EXPECTED_COUNT));
            return list;
        };

        final BiConsumer<String, Integer> assertAuthenticatorTransport = (transport, number) -> {
            List<WebAuthnLoginPage.WebAuthnAuthenticatorItem> list = getItems.get();
            assertThat(list, notNullValue());
            WebAuthnLoginPage.WebAuthnAuthenticatorItem item = list.get(number - 1);

            assertThat(item, notNullValue());
            assertThat(item.getName(), is(AUTHENTICATOR_LABEL + number));
            assertThat(item.getTransport(), is(transport));
        };

        assertAuthenticatorTransport.accept("Internal", 1);
        assertAuthenticatorTransport.accept("Bluetooth", 2);
        assertAuthenticatorTransport.accept("NFC", 3);
        assertAuthenticatorTransport.accept("USB", 4);
        assertAuthenticatorTransport.accept("USB", 5);

        webAuthnLoginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();

        logout();
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();
        webAuthnLoginPage.assertCurrent();

        // Switch to Czech locale
        managedRealm.updateWithCleanup(r -> r
                .defaultLocale(Locale.ENGLISH.getLanguage())
                .internationalizationEnabled(true)
                .supportedLocales(Locale.ENGLISH.getLanguage(), "cs"));

        driver.driver().navigate().refresh();
        webAuthnLoginPage.selectLanguage("Čeština");

        assertAuthenticatorTransport.accept("Interní", 1);
        assertAuthenticatorTransport.accept("Bluetooth", 2);
        assertAuthenticatorTransport.accept("NFC", 3);
        assertAuthenticatorTransport.accept("USB", 4);
        assertAuthenticatorTransport.accept("USB", 5);
    }

    private void assertLocalizationIndividual(VirtualAuthenticatorOptions options, String originalName, String localizedText) {
        final Consumer<String> checkTransportName = (requiredName) -> {
            List<WebAuthnLoginPage.WebAuthnAuthenticatorItem> items = webAuthnLoginPage.getItems();
            assertThat(items, notNullValue());
            assertThat(items.size(), is(1));
            assertThat(webAuthnLoginPage.getLabels(), Matchers.contains("authenticator#1"));

            WebAuthnLoginPage.WebAuthnAuthenticatorItem item = items.get(0);
            assertThat(item, notNullValue());
            assertThat(item.getTransport(), is(requiredName));
        };

        managedRealm.updateWithCleanup(r -> r
                .defaultLocale(Locale.ENGLISH.getLanguage())
                .internationalizationEnabled(true)
                .supportedLocales(Locale.ENGLISH.getLanguage(), "cs"));

        getVirtualAuthManager().useAuthenticator(options);
        addWebAuthnCredential("authenticator#1");

        final int webAuthnCount = getUserCredentialsCount();
        assertThat(webAuthnCount, is(1));

        setUpWebAuthnFlow("webAuthnFlow");
        logout();

        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();

        webAuthnLoginPage.assertCurrent();

        checkTransportName.accept(originalName);

        webAuthnLoginPage.selectLanguage("Čeština");

        checkTransportName.accept(localizedText);

        webAuthnLoginPage.clickAuthenticate();
    }

    private void addWebAuthnCredential(String label) {
        // Use the default user and add WebAuthn credential via registration flow
        registerDefaultUser(label);
    }

    private int getUserCredentialsCount() {
        UserResource user = userResource();
        List<CredentialRepresentation> credentials = user.credentials();
        return (int) credentials.stream()
                .filter(c -> WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(c.getType()))
                .count();
    }

    private void setUpWebAuthnFlow(String newFlowAlias) {
        runOnServer.run(session -> inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        runOnServer.run(session -> inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, WebAuthnAuthenticatorFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow()
        );
    }
}
