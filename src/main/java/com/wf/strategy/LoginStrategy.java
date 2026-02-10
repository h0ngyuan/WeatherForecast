package com.wf.strategy;

import com.wf.object.query.LoginQuery;

public interface LoginStrategy {

    String login(LoginQuery query);
}
