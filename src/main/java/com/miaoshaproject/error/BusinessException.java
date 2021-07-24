package com.miaoshaproject.error;

//包装器，业务异常类实现
public class BusinessException extends Exception implements CommonError{
    private CommonError commonError;

    //直接接受EmBusinessError的传参用于构造业务异常
    public BusinessException(CommonError commonError){
        super();
        this.commonError = commonError;
    }

    //接受自定义errMsg的方式构造业务异常
    public  BusinessException(CommonError commonError,String errMsg){
        super();
        this.commonError = commonError;
        this.commonError.setErrMeg(errMsg);
    }

    @Override
    public int getErrCode() {
        return this.commonError.getErrCode();
    }

    @Override
    public String getErrMeg() {
        return this.commonError.getErrMeg();
    }

    @Override
    public CommonError setErrMeg(String errMeg) {
        this.commonError.setErrMeg(errMeg);
        return this;
    }
    public CommonError getCommonError() {
        return commonError;
    }
}
