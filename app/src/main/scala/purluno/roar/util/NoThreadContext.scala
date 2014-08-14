package purluno.roar.util

import scala.concurrent.ExecutionContext

object NoThreadContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = runnable.run()

  override def reportFailure(cause: Throwable): Unit = ExecutionContext.defaultReporter
}
