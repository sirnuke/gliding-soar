package com.degrendel.glidingsoar.common

import com.degrendel.glidingsoar.common.ast.ASTNode

sealed class ValidationIssue
{
  abstract val nodes: List<ASTNode>
  abstract val message: String
}

data class ValidationError(override val nodes: List<ASTNode>, override val message: String) : ValidationIssue()
data class ValidationWarning(override val nodes: List<ASTNode>, override val message: String) : ValidationIssue()
