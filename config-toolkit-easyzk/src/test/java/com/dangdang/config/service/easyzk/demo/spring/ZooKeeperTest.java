package com.dangdang.config.service.easyzk.demo.spring;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

import static com.dangdang.config.service.easyzk.demo.spring.ZooKeeperAnnotationTarget.*;

public class ZooKeeperTest {

    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperTest.class);

    private static ClassPathXmlApplicationContext ctx;
    private static TestingServer zkTestServer;
    private static CuratorFramework cli;

    @Autowired
    private ZooKeeperAnnotationTarget annotationTarget;

    @Test
    public void test() throws Exception {
        cli.setData().forPath("/projectx/modulex/property-group1"+FIELD_PATH, "field 2".getBytes());
        annotationTarget.print("test 1");

        cli.setData().forPath("/projectx/modulex/property-group2"+METHOD_PATH, "method 2".getBytes());
        annotationTarget.print("test 2");

        cli.create().forPath("/projectx/modulex/property-group2"+NONEXISTENT_PATH, "now existent".getBytes());
        annotationTarget.print("test 3");

        Thread.sleep(5000);
        System.out.println(annotationTarget.toString());
        Thread.currentThread().join();
    }

    @Before
    public void start() throws Exception {
        zkTestServer = new TestingServer(2181);
        String zkConnect = "localhost:2181";
        cli = CuratorFrameworkFactory.newClient(zkConnect, new RetryOneTime(2000));
        cli.start();

        cli = cli.usingNamespace("test");
        cli.create().forPath("/projectx");
        cli.create().forPath("/projectx/modulex");
        cli.create().forPath("/projectx/modulex/property-group1");
        cli.create().forPath("/projectx/modulex/property-group2");
        cli.create().forPath("/projectx/modulex/property-group1/string_property_key", "1".getBytes());
        cli.create().forPath("/projectx/modulex/property-group2/int_property_key", "2".getBytes());

        cli.create().forPath("/projectx/modulex/property-group1" + FIELD_PATH, "field initial".getBytes());
        cli.create().forPath("/projectx/modulex/property-group2" + METHOD_PATH, "method initial".getBytes());

        System.setProperty("zk.connectString", zkTestServer.getConnectString());

        ctx = new ClassPathXmlApplicationContext("classpath:config-toolkit-easyzk.xml");
        ctx.start();

        annotationTarget = ctx.getBean(ZooKeeperAnnotationTarget.class);
        String field = annotationTarget.getField();
        Assert.assertEquals(field, "field initial");

        cli.setData().forPath("/projectx/modulex/property-group1" + FIELD_PATH, "liangd.chen".getBytes());
        System.out.println(annotationTarget.getField());
        //Thread.currentThread().join();
    }

    @After
    public void stop() throws IOException {
        //cli.close();
        zkTestServer.stop();

        if (ctx != null) {
            ctx.stop();
            ctx.close();
        }
    }

}
