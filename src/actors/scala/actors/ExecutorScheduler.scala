/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2005-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$

package scala.actors.scheduler

import java.util.concurrent.{ExecutorService, RejectedExecutionException}

/**
 * The <code>ExecutorScheduler</code> class uses an
 * <code>ExecutorService</code> to execute <code>Actor</code>s.
 *
 * @author Philipp Haller
 */
trait ExecutorScheduler extends IScheduler {

  protected def executor: ExecutorService

  /** Submits a <code>Runnable</code> for execution.
   *
   *  @param  task  the task to be executed
   */
  def execute(task: Runnable) {
    try {
      executor execute task
    } catch {
      case ree: RejectedExecutionException =>
        // run task on current thread
        task.run()
    }
  }

  def executeFromActor(task: Runnable) =
    execute(task)

  /** This method is called when the <code>SchedulerService</code>
   *  shuts down.
   */
  def onShutdown(): Unit =
    executor.shutdown()

  /** The scheduler is active if the underlying <code>ExecutorService</code>
   *  has not been shut down.
   */
  def isActive =
    (executor ne null) && !executor.isShutdown
}
