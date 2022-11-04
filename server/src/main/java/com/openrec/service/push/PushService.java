package com.openrec.service.push;


import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import org.springframework.stereotype.Service;

@Service
public interface PushService {

    void pushItem(ItemReq itemReq);

    void pushUser(UserReq userReq);

    void pushEvent(EventReq eventReq);
}
