package org.bigbluebutton.core.apps.groupchats

import org.bigbluebutton.common2.msgs._
import org.bigbluebutton.core.bus.MessageBus
import org.bigbluebutton.core.domain.MeetingState2x
import org.bigbluebutton.core.models.{ GroupChatFactory, Users2x }
import org.bigbluebutton.core.running.{ LiveMeeting, LogHelper }

trait CreateGroupChatReqMsgHdlr {

  def handle(msg: CreateGroupChatReqMsg, state: MeetingState2x,
             liveMeeting: LiveMeeting, bus: MessageBus): MeetingState2x = {

    def makeHeader(name: String, meetingId: String, userId: String): BbbClientMsgHeader = {
      BbbClientMsgHeader(name, meetingId, userId)
    }

    def makeEnvelope(msgType: String, name: String, meetingId: String, userId: String): BbbCoreEnvelope = {
      val routing = Routing.addMsgToClientRouting(msgType, meetingId, userId)
      BbbCoreEnvelope(name, routing)
    }

    def buildCreateGroupChatRespMsg(meetingId: String, userId: String, chatId: String, name: String,
                                    access: String, correlationId: String): BbbCommonEnvCoreMsg = {
      val envelope = makeEnvelope(MessageTypes.DIRECT, CreateGroupChatRespMsg.NAME, meetingId, userId)
      val header = makeHeader(CreateGroupChatRespMsg.NAME, meetingId, userId)

      val body = CreateGroupChatRespMsgBody(correlationId, chatId, name, access)
      val event = CreateGroupChatRespMsg(header, body)

      BbbCommonEnvCoreMsg(envelope, event)
    }

    val newState = for {
      user <- Users2x.findWithIntId(liveMeeting.users2x, msg.body.requesterId)
      gcId = GroupChatFactory.genId()
      gcUser = GroupChatUser(msg.body.requesterId, user.name)
      gc = GroupChatFactory.create(gcId, msg.body.name, msg.body.access, gcUser)
      groupChats = state.groupChats.add(gc)
      newState = state.update(groupChats)
    } yield {
      val respMsg = buildCreateGroupChatRespMsg(liveMeeting.props.meetingProp.intId, msg.body.requesterId,
        gcId, gc.name, gc.access, msg.body.correlationId)
      bus.outGW.send(respMsg)
      newState
    }

    newState.getOrElse(state)
  }
}
