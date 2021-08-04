package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
//import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")
//@CrossOrigin
public class UserController extends BaseController{
    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;





    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone")String telphone,
                                      @RequestParam(name = "otpCode")String otpCode,
                                      @RequestParam(name = "name")String name,
                                      @RequestParam(name = "gender")Integer gender,
                                      @RequestParam(name = "age")Integer age,
                                      @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
    //验证手机号和对应的otpcode相符合
    String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
    if(!com.alibaba.druid.util.StringUtils.equals(otpCode,inSessionOtpCode)){
        throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不符合");
    }
    //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.EncodeByMd5(password));


        userService.register(userModel);
        return CommonReturnType.create(null);
    }
    public  String EncodeByMd5(String str) throws NoSuchAlgorithmException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
//        BASE64Encoder base64en = new BASE64Encoder();
        //BASE64Encoder使用mvn clean package 报错

//        Base64.Encoder base64en = Base64.getMimeEncoder();
//        byte[] tmp = base64en.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        //加密字符串
 //       String newstr = base64en.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
//        String newstr = tmp.toString();


        //修改一下方法使密码验证可以使用 mvn clean package 管理工具
        Base64.Encoder base64en = Base64.getMimeEncoder();
        String newstr = base64en.encodeToString(md5.digest(str.getBytes(StandardCharsets.UTF_8)));

        return newstr;
    }

    //用户获取opt短信接口
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone")String telphone){
        //需要按照一定规则生成OPT验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;

        String optCode = String.valueOf(randomInt);


        //将OTP验证码同对应的用户手机号关联，使用httpsesession的方式和optcode绑定
        httpServletRequest.getSession().setAttribute(telphone,optCode);




        //将OTP验证码通过短信通道发送给用户,省略
        System.out.println("telphone = "+telphone+" & otpCode = "+optCode);

        return CommonReturnType.create(null);
    }




    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id")Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        //若获取的对应用户信息不存在
        if(userModel == null){
           throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //userModel = null;
        //将核心领域模型用户对象转化为可供UI使用的viewobject
        UserVO userVO = convertFromModel(userModel);

        //返回通用对象
        return CommonReturnType.create(userVO);
    }
    private UserVO convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }
    //用户登陆接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telphone")String telphone,
                                  @RequestParam(name = "password")String password) throws BusinessException, NoSuchAlgorithmException {
        //入参校验
        if(StringUtils.isEmpty(telphone)
                ||StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        //用户登陆服务，用了校验用户登陆是否合法
        UserModel userModel = userService.validateLogin(telphone, this.EncodeByMd5(password));

        //修改成若用户登陆验证成功后将对应的登陆信息和登陆凭证一起存入redis中

        //生成登陆凭证token，UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-","");
        //建立token和用户登陆态之间的联系

        redisTemplate.opsForValue().set(uuidToken,userModel);

        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);

        //将登陆凭证加入到用户登陆成功的session内
/*        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);*/

        //下发了token
        return CommonReturnType.create(uuidToken);
    }


}
