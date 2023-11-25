import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the

fun Project.registerTask(name: String): Task {
  return tasks.register(name).get()
}

fun Project.registerTask(name: String, action: (Task) -> Unit) {
  Action<Task> { action(this) }.execute(tasks.register(name).get())
}

fun Project.getTaskNamed(name: String): Task = tasks.named(name).get()

private fun Project.getProject(name: String?) = (name?.let { findProject(":$it") } ?: this)

fun Project.runAfterEvaluate(proj: String? = null, action: (Project) -> Unit) {
  getProject(proj).afterEvaluate(action)
}

fun Project.configureTask(name: String, proj: String? = null, action: (Task) -> Unit) {
  runAfterEvaluate(proj) { action.invoke(it.getTaskNamed(name)) }
}

fun Project.doFirstly(task: String, proj: String? = null, action: (Task) -> Unit) {
  runAfterEvaluate(proj) { it.getTaskNamed(task).doFirstly(action) }
}

fun Project.doLastly(task: String, proj: String? = null, action: (Task) -> Unit) {
  runAfterEvaluate(proj) { it.getTaskNamed(task).doLastly(action) }
}

fun Task.doFirstly(action: (Task) -> Unit): Task = doFirst { action(this) }

fun Task.doLastly(action: (Task) -> Unit): Task = doLast { action(this) }

fun Task.runsAfter(vararg path: Any) {
  dependsOn(path)
  mustRunAfter(path)
}

fun Task.hasExtra(name: String) = extra().has(name)

fun Task.getExtra(name: String) = extra().get(name)!!

@Suppress("UNCHECKED_CAST") fun <T> Task.getExtraAs(name: String): T = getExtra(name) as T

fun Task.getIntExtra(name: String) = getExtra(name) as Int

fun Task.getStringExtra(name: String) = getExtra(name) as String

fun Task.getStringArrayExtra(name: String) = getExtraAs<Array<String>>(name)

fun Task.setExtra(name: String, value: Any) = extra().set(name, value)

fun Project.hasProjExtra(name: String) = extra().has(name)

fun Project.getProjExtra(name: String) = extra().get(name)!!

@Suppress("UNCHECKED_CAST")
fun <T> Project.getProjExtraAs(name: String, proj: String? = null): T {
  return getProject(proj).getProjExtra(name) as T
}

fun Project.getProjIntExtra(name: String) = getProjExtra(name) as Int

fun Project.getProjStringExtra(name: String) = getProjExtra(name) as String

fun Project.getProjStringArrayExtra(name: String) = getProjExtraAs<Array<String>>(name)

fun Project.setProjExtra(name: String, value: Any) = extra().set(name, value)

private fun ExtensionAware.extra() = extensions.extraProperties

/*
 * First make version catalogs available to
 * convention plugins in root build.gradle.kts
 * https://github.com/gradle/gradle/issues/15383
 *
 * Alternative:
 * https://docs.gradle.org/current/userguide/platforms.html#sub:type-unsafe-access-to-catalog
 */
val Project.libs
  get() = the<LibrariesForLibs>()
