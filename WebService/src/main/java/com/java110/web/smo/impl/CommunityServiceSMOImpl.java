package com.java110.web.smo.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.AttrCdConstant;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.PrivilegeCodeConstant;
import com.java110.utils.constant.ServiceConstant;
import com.java110.utils.util.Assert;
import com.java110.core.context.IPageData;
import com.java110.core.component.BaseComponentSMO;
import com.java110.web.smo.ICommunityServiceSMO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.util.StringUtils;

/**
 * 小区服务类
 */

@Service("communityServiceSMOImpl")
public class CommunityServiceSMOImpl extends BaseComponentSMO implements ICommunityServiceSMO {

    private static Logger logger = LoggerFactory.getLogger(CommunityServiceSMOImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public ResponseEntity<String> listMyCommunity(IPageData pd) {
        ResponseEntity<String> responseEntity = null;
        JSONObject _paramObj = JSONObject.parseObject(pd.getReqData());
        //权限校验
        checkUserHasPrivilege(pd, restTemplate, PrivilegeCodeConstant.PRIVILEGE_ENTER_COMMUNITY);
        responseEntity = super.getStoreInfo(pd, restTemplate);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        Assert.jsonObjectHaveKey(responseEntity.getBody().toString(), "storeId", "根据用户ID查询商户ID失败，未包含storeId节点");

        String storeId = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeId");
        String storeTypeCd = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeTypeCd");

        //修改用户信息
        responseEntity = this.callCenterService(restTemplate, pd, "",
                ServiceConstant.SERVICE_API_URL + "/api/query.myCommunity.byMember?memberId=" + storeId +
                        "&memberTypeCd=" + MappingCache.getValue(MappingConstant.DOMAIN_STORE_TYPE_2_COMMUNITY_MEMBER_TYPE, storeTypeCd),
                HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        JSONArray tmpCommunitys = JSONObject.parseObject(responseEntity.getBody().toString()).getJSONArray("communitys");
        freshCommunityAttr(tmpCommunitys);
        responseEntity = new ResponseEntity<String>(tmpCommunitys.toJSONString(),
                HttpStatus.OK);
        return responseEntity;
    }

    /**
     * 查询未入驻的小区
     *
     * @param pd
     * @return
     */
    @Override
    public ResponseEntity<String> listNoEnterCommunity(IPageData pd) {
        ResponseEntity<String> responseEntity = null;
        JSONObject _paramObj = JSONObject.parseObject(pd.getReqData());
        //权限校验
        checkUserHasPrivilege(pd, restTemplate, PrivilegeCodeConstant.PRIVILEGE_ENTER_COMMUNITY);
        responseEntity = super.getStoreInfo(pd, restTemplate);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        Assert.jsonObjectHaveKey(responseEntity.getBody().toString(), "storeId", "根据用户ID查询商户ID失败，未包含storeId节点");

        String storeId = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeId");
        String storeTypeCd = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeTypeCd");
        String communityName = !_paramObj.containsKey("communityName") ? "" : _paramObj.getString("communityName");
        //修改用户信息
        if (StringUtils.isEmpty(communityName)) {
            responseEntity = this.callCenterService(restTemplate, pd, "",
                    ServiceConstant.SERVICE_API_URL + "/api/query.noEnterCommunity.byMember?memberId=" + storeId +
                            "&memberTypeCd=" + MappingCache.getValue(MappingConstant.DOMAIN_STORE_TYPE_2_COMMUNITY_MEMBER_TYPE, storeTypeCd),
                    HttpMethod.GET);
        } else {
            responseEntity = this.callCenterService(restTemplate, pd, "",
                    ServiceConstant.SERVICE_API_URL + "/api/query.noEnterCommunity.byMemberAndName?memberId=" + storeId +
                            "&memberTypeCd=" + MappingCache.getValue(MappingConstant.DOMAIN_STORE_TYPE_2_COMMUNITY_MEMBER_TYPE, storeTypeCd)
                            + "&name=" + communityName,
                    HttpMethod.GET);
        }

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }

        JSONArray tmpCommunitys = JSONObject.parseObject(responseEntity.getBody().toString()).getJSONArray("communitys");
        freshCommunityAttr(tmpCommunitys);
        responseEntity = new ResponseEntity<String>(tmpCommunitys.toJSONString(),
                HttpStatus.OK);
        return responseEntity;
    }


    /**
     * 商户入驻申请接口
     *
     * @param pd
     * @return
     */
    @Override
    public ResponseEntity<String> _saveEnterCommunity(IPageData pd) {

        ResponseEntity<String> responseEntity = null;
        Assert.jsonObjectHaveKey(pd.getReqData(), "communityId", "请求信息中未包含communityId");
        JSONObject _paramObj = JSONObject.parseObject(pd.getReqData());

        String communityId = _paramObj.getString("communityId");

        //权限校验
        checkUserHasPrivilege(pd, restTemplate, PrivilegeCodeConstant.PRIVILEGE_ENTER_COMMUNITY);
        responseEntity = super.getStoreInfo(pd, restTemplate);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        Assert.jsonObjectHaveKey(responseEntity.getBody().toString(), "storeId", "根据用户ID查询商户ID失败，未包含storeId节点");

        String storeId = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeId");
        String storeTypeCd = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeTypeCd");
        _paramObj.put("memberId", storeId);
        _paramObj.put("memberTypeCd", MappingCache.getValue(MappingConstant.DOMAIN_STORE_TYPE_2_COMMUNITY_MEMBER_TYPE, storeTypeCd));

        responseEntity = this.callCenterService(restTemplate, pd, _paramObj.toJSONString(),
                ServiceConstant.SERVICE_API_URL + "/api/member.join.community",
                HttpMethod.POST);

        return responseEntity;
    }

    /**
     * 退出小区
     *
     * @param pd
     * @return
     */
    @Override
    public ResponseEntity<String> exitCommunity(IPageData pd) {
        ResponseEntity<String> responseEntity = null;
        Assert.jsonObjectHaveKey(pd.getReqData(), "communityId", "请求信息中未包含communityId");
        JSONObject _paramObj = JSONObject.parseObject(pd.getReqData());

        String communityId = _paramObj.getString("communityId");

        Assert.hasLength(communityId, "请求报文中communityId为空");

        //权限校验
        checkUserHasPrivilege(pd, restTemplate, PrivilegeCodeConstant.PRIVILEGE_ENTER_COMMUNITY);
        responseEntity = super.getStoreInfo(pd, restTemplate);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        Assert.jsonObjectHaveKey(responseEntity.getBody().toString(), "storeId", "根据用户ID查询商户ID失败，未包含storeId节点");

        String storeId = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeId");
        String storeTypeCd = JSONObject.parseObject(responseEntity.getBody().toString()).getString("storeTypeCd");
        JSONObject paramInObj = new JSONObject();
        paramInObj.put("communityId", communityId);
        paramInObj.put("memberId", storeId);
        paramInObj.put("memberTypeCd", MappingCache.getValue(MappingConstant.DOMAIN_STORE_TYPE_2_COMMUNITY_MEMBER_TYPE, storeTypeCd));

        responseEntity = this.callCenterService(restTemplate, pd, paramInObj.toJSONString(),
                ServiceConstant.SERVICE_API_URL + "/api/member.quit.community",
                HttpMethod.POST);

        return responseEntity;
    }


    private void freshCommunityAttr(JSONArray community) {
        for (int _communityIndex = 0; _communityIndex < community.size(); _communityIndex++) {
            JSONObject _community = community.getJSONObject(_communityIndex);
            if (!_community.containsKey("attrs")) {
                continue;
            }
            JSONArray _attrs = _community.getJSONArray("attrs");
            for (int _cAttrIndex = 0; _cAttrIndex < _attrs.size(); _cAttrIndex++) {
                if (AttrCdConstant.SPEC_CD_COMMUNITY_TEL.equals(_attrs.getJSONObject(_cAttrIndex).getString("specCd"))) {
                    _community.put("tel", _attrs.getJSONObject(_cAttrIndex).getString("value"));
                }
            }

        }

    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
