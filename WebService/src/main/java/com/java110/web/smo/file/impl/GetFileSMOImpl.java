package com.java110.web.smo.file.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.context.IPageData;
import com.java110.entity.component.ComponentValidateResult;
import com.java110.utils.constant.PrivilegeCodeConstant;
import com.java110.utils.constant.ServiceConstant;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.Base64Convert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.core.component.BaseComponentSMO;
import com.java110.web.smo.file.IGetFileSMO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

/**
 * 查询notice服务类
 */
@Service("getFileSMOImpl")
public class GetFileSMOImpl extends BaseComponentSMO implements IGetFileSMO {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public ResponseEntity<Object> getFile(IPageData pd) throws SMOException,IOException {
        JSONObject paramIn = JSONObject.parseObject(pd.getReqData());

        Assert.hasKeyAndValue(paramIn, "communityId", "请求报文中未包含小区ID");
        Assert.hasKeyAndValue(paramIn, "fileId", "请求报文中未包含文件ID");

        super.checkUserHasPrivilege(pd, restTemplate, PrivilegeCodeConstant.GET_FILE);

        ComponentValidateResult result = super.validateStoreStaffCommunityRelationship(pd, restTemplate);

        Map paramMap = BeanConvertUtil.beanCovertMap(result);
        paramIn.putAll(paramMap);

        String apiUrl = ServiceConstant.SERVICE_API_URL + "/api/file.getFile"+mapToUrlParam(paramIn);


        ResponseEntity<String> responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(),
                apiUrl,
                HttpMethod.GET);

        //处理文件下载功能
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return new ResponseEntity<Object>(responseEntity.getBody(),responseEntity.getStatusCode());
        }

        JSONObject outParam = JSONObject.parseObject(responseEntity.getBody());
        MultiValueMap headers = new HttpHeaders();
        if ("jpeg".equals(outParam.getString("suffix"))) {
            headers.add("content-type", "image/jpeg");
        } else {
            headers.add("content-type", "application/octet-stream");
        }
        //headers.add("Content-Disposition", "attachment; filename=" + outParam.getString("fileName"));
        headers.add("Accept-Ranges", "bytes");

        byte[] context = Base64Convert.base64ToByte(outParam.getString("context"));

        return new ResponseEntity<Object>(context,headers,HttpStatus.OK);
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
