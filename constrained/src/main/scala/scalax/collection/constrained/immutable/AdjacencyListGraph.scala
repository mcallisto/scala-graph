package scalax.collection.constrained
package immutable

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.language.{higherKinds, postfixOps}

import scalax.collection.GraphPredef.EdgeLikeIn
import scalax.collection.immutable.{AdjacencyListGraph => SimpleAdjacencyListGraph}
import scalax.collection.config.{AdjacencyListArrayConfig, GraphConfig}
import config.GenConstrainedConfig
import PreCheckFollowUp._

trait AdjacencyListGraph[
    N, E[X] <: EdgeLikeIn[X], +This[X, Y[X] <: EdgeLikeIn[X]] <: AdjacencyListGraph[X, Y, This] with Graph[X, Y]]
    extends SimpleAdjacencyListGraph[N, E, This]
    with GraphLike[N, E, This] { this: This[N, E] =>

  protected type Config <: GraphConfig with GenConstrainedConfig with AdjacencyListArrayConfig

  override protected def initialize(nodes: Traversable[N], edges: Traversable[E[N]]) {
    withoutChecks { super.initialize(nodes, edges) }
  }

  def copy_?(nodes: Traversable[N], edges: Traversable[E[N]]): Either[ConstraintViolation, This[N, E]]

  override def +(node: N): This[N, E] = +?(node) getOrElse this

  def +?(node: N): Either[ConstraintViolation, This[N, E]] =
    checkedPlus(
      contained = nodes contains Node(node),
      preAdd = preAdd(node),
      copy = copy(nodes.toOuter.toBuffer += node, edges.toOuter),
      nodes = Set(node),
      edges = Set.empty[E[N]])

  final override protected def +#(e: E[N]): This[N, E] = +#?(e) getOrElse this

  protected def +#?(e: E[N]): Either[ConstraintViolation, This[N, E]] =
    checkedPlus(
      contained = edges contains Edge(e),
      preAdd = preAdd(e),
      copy = copy(nodes.toOuter, edges.toOuter.toBuffer += e),
      nodes = Set.empty[N],
      edges = Set(e))

  def -?(n: N): Either[ConstraintViolation, This[N, E]] = checkedMinusNode(
    n,
    forced = true,
    (outerNode: N, innerNode: NodeT) =>
      copy(nodes.toOuter.toBuffer -= outerNode, edges.toOuter.toBuffer --= (innerNode.edges map (_.toOuter))))

  final override def minusIsolated(n: N): This[N, E] = minusIsolated_?(n) getOrElse this

  def minusIsolated_?(n: N): Either[ConstraintViolation, This[N, E]] = checkedMinusNode(
    n,
    forced = false,
    (outerNode: N, innerNode: NodeT) => {
      var newNodes = nodes.toOuter.toBuffer
      var newEdges = edges.toOuter.toBuffer
      nodes.subtract(
        innerNode,
        rippleDelete = false,
        innerNode => newNodes -= outerNode,
        innerNode => newEdges --= (innerNode.edges map (_.toOuter)))
      copy(newNodes, newEdges)
    }
  )

  final override protected def -#(e: E[N]): This[N, E] = -#?(e) getOrElse this

  protected def -#?(e: E[N]): Either[ConstraintViolation, This[N, E]] = checkedMinusEdge(
    e,
    simple = true,
    (outerEdge: E[N], innerEdge: EdgeT) => copy(nodes.toOuter, edges.toOuter.toBuffer -= outerEdge))

  final override protected def -!#(e: E[N]): This[N, E] = -!#?(e) getOrElse this

  protected def -!#?(e: E[N]): Either[ConstraintViolation, This[N, E]] = checkedMinusEdge(
    e,
    simple = false,
    (outerEdge: E[N], innerEdge: EdgeT) =>
      copy(nodes.toOuter.toBuffer --= innerEdge.privateNodes map (n => n.value), edges.toOuter.toBuffer -= e))
}
