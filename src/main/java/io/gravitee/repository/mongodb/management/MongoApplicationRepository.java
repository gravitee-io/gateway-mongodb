/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.mongodb.management.internal.application.ApplicationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import io.gravitee.repository.mongodb.management.internal.model.MemberMongo;
import io.gravitee.repository.mongodb.management.internal.model.UserMongo;
import io.gravitee.repository.mongodb.management.internal.user.UserMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
@Component
public class MongoApplicationRepository implements ApplicationRepository {

	@Autowired
	private ApplicationMongoRepository internalApplicationRepo;
	
	@Autowired
	private UserMongoRepository internalUserRepo;
	
	@Autowired
	private GraviteeMapper mapper;

	@Override
	public Set<Application> findAll() throws TechnicalException {
		List<ApplicationMongo> applications = internalApplicationRepo.findAll();
		return mapApplications(applications);
	}

	@Override
	public Application create(Application application) throws TechnicalException {
		ApplicationMongo applicationMongo = mapApplication(application);
		ApplicationMongo applicationMongoCreated = internalApplicationRepo.insert(applicationMongo);
		return mapApplication(applicationMongoCreated);
	}

	@Override
	public Application update(Application application) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(application.getId());
		
		// Update, but don't change invariant other creation information
		applicationMongo.setName(application.getName());
		applicationMongo.setDescription(application.getDescription());
		applicationMongo.setUpdatedAt(application.getUpdatedAt());
		applicationMongo.setType(application.getType());

		ApplicationMongo applicationMongoUpdated = internalApplicationRepo.save(applicationMongo);
		return mapApplication(applicationMongoUpdated);
	}

	@Override
	public Optional<Application> findById(String applicationId) throws TechnicalException {
		ApplicationMongo application = internalApplicationRepo.findOne(applicationId);
		return Optional.ofNullable(mapApplication(application));
	}

	@Override
	public void delete(String applicationId) throws TechnicalException {
		internalApplicationRepo.delete(applicationId);
	}

	@Override
	public Set<Application> findByUser(String username, MembershipType membershipType) throws TechnicalException {
		return mapApplications(internalApplicationRepo.findByUser(username, membershipType));
	}

	@Override
	public void saveMember(String applicationId, String username, MembershipType membershipType) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(applicationId);
		UserMongo userMongo = internalUserRepo.findOne(username);

		Membership membership = getMember(applicationId, username);
		if (membership == null) {
			MemberMongo memberMongo = new MemberMongo();
			memberMongo.setUser(userMongo);
			memberMongo.setType(membershipType.toString());
			memberMongo.setCreatedAt(new Date());
			memberMongo.setUpdatedAt(memberMongo.getCreatedAt());

			applicationMongo.getMembers().add(memberMongo);

			internalApplicationRepo.save(applicationMongo);
		} else {
			for (MemberMongo memberMongo : applicationMongo.getMembers()) {
				if (memberMongo.getUser().getName().equalsIgnoreCase(username)) {
					memberMongo.setType(membershipType.toString());
					internalApplicationRepo.save(applicationMongo);
					break;
				}
			}
		}
	}

	@Override
	public void deleteMember(String applicationId, String username) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(applicationId);
		MemberMongo memberToDelete = null;

		for (MemberMongo memberMongo : applicationMongo.getMembers()) {
			if (memberMongo.getUser().getName().equalsIgnoreCase(username)) {
				memberToDelete = memberMongo;
			}
		}

		if (memberToDelete != null) {
			applicationMongo.getMembers().remove(memberToDelete);
			internalApplicationRepo.save(applicationMongo);
		}
	}

	@Override
	public Collection<Membership> getMembers(String applicationId, MembershipType membershipType) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(applicationId);
		List<MemberMongo> membersMongo = applicationMongo.getMembers();
		Set<Membership> members = new HashSet<>(membersMongo.size());

		for (MemberMongo memberMongo : membersMongo) {
			if (membershipType == null || (
					membershipType != null && memberMongo.getType().equalsIgnoreCase(membershipType.toString()))) {
				Membership member = new Membership();
				member.setUser(mapUser(memberMongo.getUser()));
				member.setMembershipType(MembershipType.valueOf(memberMongo.getType()));
				member.setCreatedAt(memberMongo.getCreatedAt());
				member.setUpdatedAt(memberMongo.getUpdatedAt());
				members.add(member);
			}
		}

		return members;
	}

	private User mapUser(final UserMongo userMongo) {
		final User user = new User();
		user.setUsername(userMongo.getName());
		user.setCreatedAt(userMongo.getCreatedAt());
		user.setEmail(userMongo.getEmail());
		user.setFirstname(userMongo.getFirstname());
		user.setLastname(userMongo.getLastname());
		user.setPassword(userMongo.getPassword());
		user.setUpdatedAt(userMongo.getUpdatedAt());
		if (userMongo.getRoles() != null) {
			user.setRoles(new HashSet<>(userMongo.getRoles()));
		}
		return user;
	}

	@Override
	public Membership getMember(String application, String username) throws TechnicalException {
		Collection<Membership> members = getMembers(application, null);
		for (Membership member : members) {
			if (member.getUser().getUsername().equalsIgnoreCase(username)) {
				return member;
			}
		}

		return null;
	}

	private Set<Application> mapApplications(Collection<ApplicationMongo> applications){
		return applications.stream().map(this::mapApplication).collect(Collectors.toSet());
	}
	
	private Application mapApplication(ApplicationMongo applicationMongo) {
		return (applicationMongo == null) ? null : mapper.map(applicationMongo, Application.class);
	}
	
	private ApplicationMongo mapApplication(Application application) {
		return (application == null) ? null : mapper.map(application, ApplicationMongo.class);
	}
}
