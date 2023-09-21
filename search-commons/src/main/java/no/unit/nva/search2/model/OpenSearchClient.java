package no.unit.nva.search2.model;


import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.secrets.SecretsReader;
import org.checkerframework.checker.units.qual.K;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.stream.Stream;

import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureAuthUri;

public interface OpenSearchClient<R,Q extends OpenSearchQuery<K extends Enum<K> & ParameterKey,R>>   {

    static OpenSearchClient<R, Q> defaultClient(){
        return null;
    }

    @NotNull
    static Stream<UsernamePasswordWrapper> getUsernamePasswordStream(SecretsReader secretsReader) {
        return Stream.of(
            secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class));
    }
    @NotNull
    static CognitoCredentials getCognitoCredentials(UsernamePasswordWrapper wrapper) {
        var uri = URI.create(readSearchInfrastructureAuthUri());
        return new CognitoCredentials(wrapper::getUsername, wrapper::getPassword, uri);
    }

    R doSearch(Q query, String mediaType);



}
