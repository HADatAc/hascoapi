package module;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import akka.actor.ActorSystem;
import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;

@Singleton
public class MyActorSystem {

	@Inject
	private ActorSystem system;

	@Inject
	public MyActorSystem(ActorSystem system) {
		this.system = system;
		schedule();
	}

	public void schedule() {

        //Runnable processMessages = new Runnable() {
        //    @Override
        //    public void run() {
        //        MessageWorker.exec();
        //    }
        //};

		/*
		system.scheduler().schedule(
                FiniteDuration.create(0, TimeUnit.SECONDS), 
                //FiniteDuration.create(15, TimeUnit.SECONDS),
                FiniteDuration.create(150, TimeUnit.MILLISECONDS),
                processMessages, system.dispatcher());
        */
		
		/*
		system.scheduler().schedule(
                FiniteDuration.create(0, TimeUnit.SECONDS), 
                FiniteDuration.create(5, TimeUnit.SECONDS), 
                scanning, system.dispatcher());
		
		system.scheduler().schedule(
                FiniteDuration.create(0, TimeUnit.SECONDS), 
                FiniteDuration.create(30, TimeUnit.SECONDS),
                workingfiles, system.dispatcher());
        */
	}
}
