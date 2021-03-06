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
package org.apache.openmeetings.webservice;

import static org.apache.openmeetings.db.dto.basic.ServiceResult.NO_PERMISSION;
import static org.apache.openmeetings.util.OpenmeetingsVariables.webAppRootKey;
import static org.apache.openmeetings.webservice.Constants.TNS;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.feature.Features;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.openmeetings.core.data.file.FileProcessor;
import org.apache.openmeetings.db.dao.file.FileExplorerItemDao;
import org.apache.openmeetings.db.dto.basic.ServiceResult;
import org.apache.openmeetings.db.dto.basic.ServiceResult.Type;
import org.apache.openmeetings.db.dto.file.FileExplorerItemDTO;
import org.apache.openmeetings.db.dto.file.FileExplorerObject;
import org.apache.openmeetings.db.entity.file.FileExplorerItem;
import org.apache.openmeetings.db.entity.server.Sessiondata;
import org.apache.openmeetings.db.entity.user.User.Right;
import org.apache.openmeetings.db.util.AuthLevelUtil;
import org.apache.openmeetings.util.process.ConverterProcessResultList;
import org.apache.openmeetings.webservice.error.ServiceException;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * Contains methods to import and upload files into the Files section of the
 * conference room and the personal drive of any user
 *
 * @author sebawagner
 *
 */
@WebService(serviceName="org.apache.openmeetings.webservice.FileWebService", targetNamespace = TNS)
@Features(features = "org.apache.cxf.feature.LoggingFeature")
@Produces({MediaType.APPLICATION_JSON})
@Path("/file")
public class FileWebService extends BaseWebService {
	private static final Logger log = Red5LoggerFactory.getLogger(FileWebService.class, webAppRootKey);

	private static FileExplorerItemDao getDao() {
		return getBean(FileExplorerItemDao.class);
	}

	/**
	 * deletes files or folders based on it id
	 *
	 * @param sid
	 *            The SID of the User. This SID must be marked as logged in
	 * @param id
	 *            the id of the file or folder
	 * @return - id of the file deleted, error code otherwise
	 * @throws ServiceException
	 */
	@DELETE
	@Path("/{id}")
	public ServiceResult delete(@QueryParam("sid") @WebParam(name="sid") String sid, @PathParam("id") @WebParam(name="id") Long id) throws ServiceException {
		try {
			Sessiondata sd = check(sid);

			FileExplorerItemDao dao = getDao();
			FileExplorerItem f = dao.get(id);
			if (f == null) {
				return new ServiceResult(-1L, "Bad id", Type.ERROR);
			}
			Long userId = sd.getUserId();
			Set<Right> rights = getRights(userId);
			if (AuthLevelUtil.hasWebServiceLevel(rights)
				|| (AuthLevelUtil.hasUserLevel(rights) && userId.equals(f.getOwnerId())))
			{
				dao.delete(f);
				return new ServiceResult(id, "Deleted", Type.SUCCESS);
			} else {
				return NO_PERMISSION;
			}
		} catch (Exception e) {
			log.error("[delete] ", e);
			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 *
	 * deletes a file by its external Id and type
	 *
	 * @param sid
	 *            The SID of the User. This SID must be marked as logged in
	 * @param externalId
	 *            the od of the file or folder
	 * @param externalType
	 *            the externalType
	 * @return - null
	 */
	@DELETE
	@Path("/{externaltype}/{externalid}")
	public ServiceResult deleteExternal(
			@WebParam(name="sid") @QueryParam("sid") String sid
			, @WebParam(name="externaltype") @PathParam("externaltype") String externalType
			, @WebParam(name="externalid") @PathParam("externalid") String externalId
			)
	{
		try {
			if (AuthLevelUtil.hasWebServiceLevel(getRights(sid))) {
				FileExplorerItemDao dao = getDao();
				FileExplorerItem f = dao.get(externalId, externalType);
				dao.delete(f);
				return new ServiceResult(f.getId(), "Deleted", Type.SUCCESS);
			}
		} catch (Exception err) {
			log.error("[deleteFileOrFolderByExternalIdAndType]", err);
		}
		return null;
	}

	/**
	 * to add a folder to the private drive, set parentFileExplorerItemId = 0 and isOwner to 1/true and
	 * externalUserId/externalUserType to a valid user
	 *
	 * @param sid
	 *            The SID of the User. This SID must be marked as logged in
	 * @param file
	 *            the The file to be added
	 * @param stream
	 *            the The file to be added
	 * @return - Object created
	 * @throws ServiceException
	 */
	@WebMethod
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("/")
	public FileExplorerItemDTO add(@WebParam(name="sid") @QueryParam("sid") String sid
			, @Multipart(value = "file", type = MediaType.APPLICATION_JSON) @WebParam(name="file") FileExplorerItemDTO file
			, @Multipart(value = "stream", type = MediaType.APPLICATION_OCTET_STREAM, required = false) @WebParam(name="stream") InputStream stream
			) throws ServiceException
	{
		try {
			Sessiondata sd = check(sid);
			Long userId = sd.getUserId();

			FileExplorerItem f = file == null ? null : file.get();
			if (f == null || f.getId() != null) {
				throw new ServiceException("Bad id");//TODO err code -1 ????
			}
			Set<Right> rights = getRights(userId);
			/* FIXME TODO permissions
			if (AuthLevelUtil.hasWebServiceLevel(rights)
					|| (AuthLevelUtil.hasUserLevel(rights) && userId.equals(f.getOwnerId())))*/
			if (AuthLevelUtil.hasUserLevel(rights))
			{
				f.setInsertedBy(userId);
				//TODO permissions
				if (stream != null) {
					//TODO attachment
					ConverterProcessResultList result = getBean(FileProcessor.class).processFile(f, stream);
					if (result.hasError()) {
						throw new ServiceException(result.getLogMessage());
					}
				} else {
					f = getDao().update(f);
				}
				return new FileExplorerItemDTO(f);
			} else {
				throw new ServiceException("Insufficient permissions"); //TODO code -26
			}
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("[add]", e);
			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 * Get a File Explorer Object by a given Room
	 *
	 * @param sid
	 *            The SID of the User. This SID must be marked as logged in
	 * @param roomId
	 *            Room Id
	 * @return - File Explorer Object by a given Room
	 * @throws ServiceException
	 */
	@WebMethod
	@GET
	@Path("/room/{id}")
	public FileExplorerObject getRoom(@WebParam(name="sid") @QueryParam("sid") String sid
			, @WebParam(name="id") @PathParam("id") long roomId
			) throws ServiceException
	{
		try {
			Sessiondata sd = check(sid);
			Long userId = sd.getUserId();

			if (AuthLevelUtil.hasUserLevel(getRights(userId))) {
				log.debug("roomId " + roomId);

				FileExplorerItemDao dao = getDao();
				FileExplorerObject fileExplorerObject = new FileExplorerObject();

				// Home File List
				List<FileExplorerItem> fList = dao.getByOwner(userId);
				fileExplorerObject.setUser(fList, dao.getSize(fList));

				// Public File List
				List<FileExplorerItem> rList = dao.getByRoom(roomId);
				fileExplorerObject.setRoom(rList, dao.getSize(rList));

				return fileExplorerObject;
			} else {
				throw new ServiceException("Insufficient permissions"); //TODO code -26
			}
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("[getRoom]", e);
			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 *
	 * Get list of {@link FileExplorerItemDTO} by parent
	 *
	 * @param sid
	 *            SID The SID of the User. This SID must be marked as logged in
	 * @param parentId
	 *            the parent folder id
	 * @param roomId
	 *            the room id
	 * @return - list of file explorer items
	 * @throws ServiceException
	 */
	@WebMethod
	@GET
	@Path("/room/{id}/{parent}")
	public List<FileExplorerItemDTO> getRoomByParent(@WebParam(name="sid") @QueryParam("sid") String sid
			, @WebParam(name="id") @PathParam("id") long roomId
			, @WebParam(name="parent") @PathParam("parent") long parentId
			) throws ServiceException
	{
		try {
			Sessiondata sd = check(sid);
			Long userId = sd.getUserId();

			if (AuthLevelUtil.hasUserLevel(getRights(userId))) {
				log.debug("getRoomByParent " + parentId);

				FileExplorerItemDao dao = getDao();
				List<FileExplorerItem> list = new ArrayList<>();
				if (parentId < 0) {
					if (parentId == -1) {
						list = dao.getByOwner(userId);
					} else {
						list = dao.getByRoom(roomId);
					}
				} else {
					list = dao.getByParent(parentId);
				}
				return FileExplorerItemDTO.list(list);
			} else {
				throw new ServiceException("Insufficient permissions"); //TODO code -26
			}
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("[getRoom]", e);
			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 *
	 * update a file or folder name
	 *
	 * @param sid
	 *            SID The SID of the User. This SID must be marked as logged in
	 * @param id
	 *            file or folder id
	 * @param name
	 *            new file or folder name
	 * @return - resulting file object
	 * @throws ServiceException
	 */
	@WebMethod
	@POST
	@Path("/rename/{id}/{name}")
	public FileExplorerItemDTO rename(@WebParam(name="sid") @QueryParam("sid") String sid
			, @WebParam(name="id") @PathParam("id") long id
			, @WebParam(name="name") @PathParam("name") String name) throws ServiceException
	{
		try {
			if (AuthLevelUtil.hasUserLevel(getRights(sid))) {
				// FIXME TODO: check if this user is allowed to change this file

				log.debug("rename " + id);
				FileExplorerItem f = getDao().rename(id, name);
				return f == null ? null : new FileExplorerItemDTO(f);
			} else {
				throw new ServiceException("Insufficient permissions"); //TODO code -26
			}
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("[rename] ", e);
			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 * move a file or folder
	 *
	 * @param sid
	 *            SID The SID of the User. This SID must be marked as logged in
	 * @param id
	 *            current file or folder id to be moved
	 * @param parentId
	 *            new parent folder id
	 * @return - resulting file object
	 * @throws ServiceException
	 */
	@WebMethod
	@POST
	@Path("/move/{roomid}/{id}/{parentid}")
	public FileExplorerItemDTO move(@WebParam(name="sid") @QueryParam("sid") String sid
			, @WebParam(name="id") @PathParam("id") long id
			, @WebParam(name="roomid") @PathParam("roomid") long roomId
			, @WebParam(name="parentid") @PathParam("parentid") long parentId) throws ServiceException
	{
		try {
			Sessiondata sd = check(sid);
			Long userId = sd.getUserId();
			if (AuthLevelUtil.hasUserLevel(getRights(userId))) {
				// FIXME TODO A test is required that checks if the user is allowed to move the file
				log.debug("move " + id);
				FileExplorerItem f = getDao().move(id, parentId, userId, roomId);
				return f == null ? null : new FileExplorerItemDTO(f);
			} else {
				throw new ServiceException("Insufficient permissions"); //TODO code -26
			}
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("[move] ", e);
			throw new ServiceException(e.getMessage());
		}
	}

}
