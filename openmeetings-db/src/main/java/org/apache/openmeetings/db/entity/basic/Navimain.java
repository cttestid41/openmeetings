/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.db.entity.basic;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.openmeetings.db.entity.IDataProviderEntity;

@Entity
@Table(name = "navimain")
public class Navimain implements IDataProviderEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "global_id")
	private Long globalId;

	@Column(name = "name")
	private String name;

	@Column(name = "icon")
	private String icon;

	@Column(name = "action")
	private String action;

	@Column(name = "params")
	private String params;

	@Column(name = "updated")
	private Date updated;

	@Column(name = "inserted")
	private Date inserted;

	@Column(name = "comment")
	private String comment;

	@Column(name = "naviorder", nullable = false)
	private int naviorder;

	@Column(name = "level_id", nullable = false)
	private int levelId;

	@Column(name = "label_id")
	private String labelId;

	@Column(name = "deleted", nullable = false)
	private boolean deleted;

	@Column(name = "tooltip_label_id")
	private String tooltipLabelId;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public int getLevelId() {
		return levelId;
	}

	public void setLevelId(int levelId) {
		this.levelId = levelId;
	}

	public Long getGlobalId() {
		return globalId;
	}

	public void setGlobalId(Long globalId) {
		this.globalId = globalId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Date getInserted() {
		return inserted;
	}

	public void setInserted(Date inserted) {
		this.inserted = inserted;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNaviorder() {
		return naviorder;
	}

	public void setNaviorder(int naviorder) {
		this.naviorder = naviorder;
	}

	public String getLabelId() {
		return labelId;
	}

	public void setLabelId(String labelId) {
		this.labelId = labelId;
	}

	public String getTooltipLabelId() {
		return tooltipLabelId;
	}

	public void setTooltipLabelId(String tooltipLabelId) {
		this.tooltipLabelId = tooltipLabelId;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	@Override
	public String toString() {
		return "Navimain [id=" + id + ", globalId=" + globalId + ", name=" + name + ", action=" + action + ", params=" + params
				+ ", naviorder=" + naviorder + ", labelId=" + labelId + ", deleted=" + deleted + ", tooltipLabelId="
				+ tooltipLabelId + "]";
	}
}
