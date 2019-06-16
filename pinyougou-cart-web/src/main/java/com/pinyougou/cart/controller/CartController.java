package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.pojogroup.Cart;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import util.CookieUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Reference(timeout = 60000)
    public CartService cartService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;
    /**
     * 购物车列表
     * @param
     * @return
     */
    @RequestMapping("/findCartList")
    public List<Cart> findCartList(){
        //得到登陆人账号,判断当前是否有人登陆
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登录人:"+username);
        //从cookie中提取购物车
        String cartListString = util.CookieUtil.getCookieValue(request, "cartList", "UTF-8");
        if (cartListString==null||cartListString.equals("")){
            cartListString="[]";
        }
        List<Cart> cartList_cookie = JSON.parseArray(cartListString, Cart.class);

        if (username.equals("anonymousUser")){//如果未登录
            System.out.println("从cookie中提取购物车");

            return cartList_cookie;
        }else {//如果登录
            //获取redis购物车
            List<Cart> cartList_redis = cartService.findCartListFromRedis(username);
            if(cartList_cookie.size()>0){//如果本地存在购物车
                //合并购物车
                List<Cart> cartList = cartService.mergeCartList(cartList_cookie, cartList_redis);
                //将合并后的购物车存入redis
                cartService.saveCartListToRedis(username,cartList);
                util.CookieUtil.deleteCookie(request,response,"cartList");
                System.out.println("执行了合并购物车逻辑!");
                return cartList;
            }
            return cartList_redis;
        }
    }
    /**
     * 添加商品到购物车
     * @param
     * @param
     * @param itemId
     * @param num
     * @return
     */
    @RequestMapping("/addGoodsToCartList")
    public Result addGoodsToCartList(Long itemId,Integer num){
        //得到登陆人账号,判断当前是否有人登陆
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登录人:"+username);

        try {
            //获取购物车列表
            List<Cart> cartList = findCartList();
            //调用服务方法操作购物车
            cartList = cartService.addGoodsToCartList(cartList, itemId, num);
            if (username.equals("anonymousUser")){//如果是未登录，
                //将新的购物车存入cookie
                String cartListString=JSON.toJSONString(cartList);
                CookieUtil.setCookie(request,response, "cartList", JSON.toJSONString(cartList),3600*24,"UTF-8");
                System.out.println("向cookie存入数据");

            }else {//如果登录
                cartService.saveCartListToRedis(username,cartList);
            }
           return new Result(true,"存入购物车成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"存入购物车失败!");
        }

    }

}
