/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chainofhandlers.BodyHandler;
import chainofhandlers.Handler;
import chainofhandlers.HeartbeatHandler;
//import chainofhandlers.RequestVoteHandler;
//import chainofhandlers.VoteHandler;
import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.common.Common.Failure;
import pipe.common.Common.Body;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import raft.FollowerState;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash
 * 
 */
public class WorkHandler extends SimpleChannelInboundHandler<WorkMessage> {
	protected static Logger logger = LoggerFactory.getLogger("work");
	protected ServerState state;
	protected boolean debug = false;
	private Handler handler;
	private EdgeList outboundEdges;
	//plan to shift this to vote handler
	/*public int requiredVotes=1;
    Set<Integer> voteList=new HashSet<Integer>();*/
	public WorkHandler(ServerState state) {
		if (state != null) {
			this.state = state;
			this.handler=new BodyHandler(state);
			Handler heartbeatHandler= new HeartbeatHandler(state);
			//Handler voteHandler= new VoteHandler(state);
			//Handler reqVoteHandler= new RequestVoteHandler(state);
			handler.setNext(heartbeatHandler);
			//handler.setNext(voteHandler);
			//handler.setNext(reqVoteHandler);
			
            
		}
		
	}

	/**
	 * override this method to provide processing behavior. T
	 * 
	 * @param msg
	 */
	
	public void handleMessage(WorkMessage msg, Channel channel) {
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}

		//if (debug)
		
			

		// TODO How can you implement this without if-else statements?
		// USE HANDLERS
		try {
			//System.out.println("im printing work now using handlers chain");
			//handler.processWorkMessage(msg, channel);
			//System.out.println("im in try");
			if(msg.hasReqvote()){
//				System.out.println("im in req vote handler");
//	        	EdgeMonitor emon=state.getEmon();  	        	
//	        	System.out.println(msg.getReqvote().getCandidateID()+" "+ msg.getReqvote().getCurrentTerm());
//	        	if(state.getCurrentTerm()<msg.getReqvote().getCurrentTerm()){
//	        	state.setCurrentTerm(msg.getReqvote().getCurrentTerm());
//	        	WorkMessage wm=emon.Vote(state.getConf().getNodeId(), msg.getReqvote().getCandidateID());
//	        	emon.sendVote(wm);
//	        	}
	        	System.out.println("it should send you to follower");
	        	state.getManager().getCurrentState().onRequestVoteReceived(msg);
	        	
			}
			
				if(msg.hasVote()){
					System.out.println(" should send you to candidate's receivedVoteReply method");		  
//		                int voterId = msg.getVote().getVoterID();
//		                logger.info("Vote Received from "+voterId );		               
//		                voteList.add(voterId);
//		                if (voteList.size() >= requiredVotes) {
//		                    state.setCurrentLeader(state.getConf().getNodeId());
//		                }
//		                System.out.println("Present Leader is "+state.getCurrentLeader());
					state.getManager().getCurrentState().receivedVoteReply(msg);
		            
				}
				
			else
			if (msg.hasBeat()) {
				System.out.println("im checking heartbeat");
//				Heartbeat hb = msg.getBeat();
//				logger.debug("heartbeat from " + msg.getHeader().getNodeId());
				state.getManager().getCurrentState().receivedHeartBeat(msg);
			} /*else if (msg.hasPing()) {
				logger.info("ping from " + msg.getHeader().getNodeId());
				boolean p = msg.getPing();
				WorkMessage.Builder rb = WorkMessage.newBuilder();
				rb.setPing(true);
				channel.write(rb.build());
			} else if (msg.hasErr()) {
				Failure err = msg.getErr();
				logger.error("failure from " + msg.getHeader().getNodeId());
				// PrintUtil.printFailure(err);
			} else if (msg.hasTask()) {
				Task t = msg.getTask();
			} else if (msg.hasState()) {
				WorkState s = msg.getState();
			}else if (msg.hasBody()) {
					PrintUtil.printBody(msg.getBody());
			}	*/		
			
		} catch (NullPointerException e) {
            logger.error("Null pointer has occured from work handler logic" + e.getMessage());
        } catch (Exception e) {
			// TODO add logging
			Failure.Builder eb = Failure.newBuilder();
			eb.setId(state.getConf().getNodeId());
			eb.setRefId(msg.getHeader().getNodeId());
			eb.setMessage(e.getMessage());
			WorkMessage.Builder rb = WorkMessage.newBuilder(msg);
			rb.setErr(eb);
			channel.write(rb.build());
		}

		System.out.flush();

	}

	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the client's thread pool to minimize the time it takes to process other
	 * messages.
	 * 
	 * @param ctx
	 *            The channel the message was received from
	 * @param msg
	 *            The message
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WorkMessage msg) throws Exception {
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}

}