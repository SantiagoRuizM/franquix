package com.franquix.franchises.domain.model;

import java.util.List;

/** Read model: a franchise with its whole branch and product tree. */
public record FranchiseOverview(Franchise franchise, List<BranchOverview> branches) {

    public FranchiseOverview {
        branches = List.copyOf(branches);
    }
}
