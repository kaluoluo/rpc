package com.swy.protocol.dubbo;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.swy.framework.MessageCallBack;
import com.swy.framework.RpcRequest;
import com.swy.protocol.Procotol;
import com.swy.framework.URL;
import com.swy.protocol.dubbo.channelpool.NettyChannelPoolFactory;
import com.swy.protocol.dubbo.channelpool.ResponseHolder;

public class DubboProcotol implements Procotol {

    @Override
    public void start(URL url) {
        NettyServer nettyServer = NettyServer.getInstance();
        nettyServer.start(url.getHost(),url.getPort());
    }


    @Override
    public Object send(URL url, RpcRequest invocation) {
        ArrayBlockingQueue<Channel> queue = NettyChannelPoolFactory.getInstance().acqiure(url);
        Channel channel = null;
        try {
            channel = queue.poll(invocation.getTimeout(), TimeUnit.MILLISECONDS);

            if(channel == null || !channel.isActive() || !channel.isOpen()|| !channel.isWritable()){
                channel = queue.poll(invocation.getTimeout(), TimeUnit.MILLISECONDS);
                if(channel == null){
                    channel = NettyChannelPoolFactory.getInstance().registerChannel(url);
                }
            }
            //将本次调用的信息写入Netty通道,发起异步调用
            ChannelFuture channelFuture = channel.writeAndFlush(invocation);
            channelFuture.syncUninterruptibly();
            MessageCallBack callback = new MessageCallBack(invocation);
            ResponseHolder.getInstance().mapCallBack.put(invocation.getRequestId(), callback);
            try {
                return callback.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }finally{
            System.out.println("release:"+channel.id());
            NettyChannelPoolFactory.getInstance().release(queue, channel, url);
        }
        return null;
    }
}
