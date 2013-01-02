package com.quantcast.policy;


public interface PolicyDAO {

    public Policy getPolicy();
    public void savePolicy(Policy policy);
    
}
