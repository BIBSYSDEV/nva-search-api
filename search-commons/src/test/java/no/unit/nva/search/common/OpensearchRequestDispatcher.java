//package no.unit.nva.search.common;
//
//import no.unit.nva.search.common.records.QueryContentWrapper;
//import no.unit.nva.search.common.records.ResponseFormatter;
//
//import java.net.URI;
//import java.util.stream.Stream;
//
//public class OpensearchRequestDispatcher implements Query<>{
//    @Override
//    public Stream<QueryContentWrapper> assemble() {
//        return Stream.empty();
//    }
//
//    @Override
//    public <R, Q extends Query<String>> ResponseFormatter<String> doSearch(OpenSearchClient<R, Q> queryClient) {
//        return null;
//    }
//
//    @Override
//    protected URI openSearchUri() {
//        return null;
//    }
//}
