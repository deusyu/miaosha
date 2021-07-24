package com.miaoshaproject.error;

public interface CommonError {
    public int getErrCode();
    public String getErrMeg();
    public CommonError setErrMeg(String errMeg);
}
