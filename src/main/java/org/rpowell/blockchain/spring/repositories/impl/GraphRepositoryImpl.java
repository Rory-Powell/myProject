package org.rpowell.blockchain.spring.repositories.impl;

import org.rpowell.blockchain.domain.*;
import org.rpowell.blockchain.graph.CypherQueries;
import org.rpowell.blockchain.network.requests.GraphRequests;
import org.rpowell.blockchain.network.responses.QueryResponse;
import org.rpowell.blockchain.spring.repositories.IGraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;

import java.util.*;


@Repository
public class GraphRepositoryImpl implements IGraphRepository {

    private static final Logger log = LoggerFactory.getLogger(GraphRepositoryImpl.class);

    protected GraphRepositoryImpl() {}

    public List<Address> getAssociatedAddresses(String address) {
        // Get the wallets associated with this address
        ResponseEntity<QueryResponse> response = GraphRequests
                .queryForObject(CypherQueries.ownerQuery(address), QueryResponse.class);

        // Use set for unique answers
        Set<Address> addressSet = new HashSet<>();

        // For each wallet
        for (Map map : getDataMaps(response)) {
            List<Integer> values = (List<Integer>) map.get("row");
            int walletId = values.get(0);

            // Get all the addresses in that wallet
            ResponseEntity<QueryResponse> response1 = GraphRequests
                    .queryForObject(CypherQueries.addressesOfOwnerQuery(walletId), QueryResponse.class);

            // Add them to the set
            for (Map map1 : getDataMaps(response1)) {
                Address address1 = new Address();
                List<String> values1 = (List<String>) map1.get("row");
                address1.setAddress(values1.get(0));
                addressSet.add(address1);
            }
        }

        // Remove the original from the results
        Address original = new Address();
        original.setAddress(address);
        addressSet.remove(original);

        return new ArrayList<>(addressSet);
    }

    /**
     * Get all the stored nodes with an address label.
     * @return  A resource iterator of address nodes.
     */
    public List<Address> getAllAddresses() {
        ResponseEntity<QueryResponse> response = GraphRequests
                .queryForObject(CypherQueries.addressesQuery(1000), QueryResponse.class);

        List<Address> addresses = new ArrayList<>();

        for (Map map : getDataMaps(response)) {
            Address address = new Address();
            List<String> values = (List<String>) map.get("row");
            address.setAddress(values.get(0));
            addresses.add(address);
        }

        return addresses;
    }

    private List<Map> getDataMaps(ResponseEntity<QueryResponse> responseEntity) {
        QueryResponse addressesResponse = responseEntity.getBody();

        ArrayList results = (ArrayList) addressesResponse.getResults();
        Map result = (Map) results.get(0);
        return (List<Map>) result.get("data");
    }
}
