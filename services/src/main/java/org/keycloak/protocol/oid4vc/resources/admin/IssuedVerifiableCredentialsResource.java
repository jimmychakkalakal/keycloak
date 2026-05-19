package org.keycloak.protocol.oid4vc.resources.admin;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialsRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

public class IssuedVerifiableCredentialsResource {

    private static final Logger logger = Logger.getLogger(UserVerifiableCredentialResource.class);

    private final AdminPermissionEvaluator auth;
    private final UserModel user;
    private final KeycloakSession session;
    private final RealmModel realm;

    public IssuedVerifiableCredentialsResource(AdminPermissionEvaluator auth, UserModel user, KeycloakSession session, RealmModel realm) {
        this.auth = auth;
        this.user = user;
        this.session = session;
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Get all verifiable credentials issued to this user")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<IssuedVerifiableCredentialsRepresentation> getIssuedCredentials() {
        auth.users().requireView(user);
        checkOid4VCIEnabled();

        return session.users().getIssuedVerifiableCredentialsByUser(user.getId())
                .map(ModelToRepresentation::toRepresentation)
                .toList();
    }

    private void checkOid4VCIEnabled() {
        if (!Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI)) {
            throw ErrorResponse.error("Feature " + Profile.Feature.OID4VC_VCI.getKey() + " not enabled", Response.Status.BAD_REQUEST);
        }
        if (!realm.isVerifiableCredentialsEnabled()) {
            throw ErrorResponse.error("Verifiable credentials not enabled for the realm", Response.Status.BAD_REQUEST);
        }
    }
}
