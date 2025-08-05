import SwiftUI

/**
 * SwiftUI view for adding new friends by email search and sending friend requests.
 * 
 * This view allows users to search for other users by email address
 * and send friend requests to them.
 */
struct AddFriendView: View {
    @StateObject private var viewModel = AddFriendViewModel()
    @Environment(\.presentationMode) var presentationMode
    @State private var email = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Email Address")
                        .font(.headline)
                    
                    TextField("Enter email address", text: $email)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }
                .padding(.horizontal)
                
                Button(action: {
                    viewModel.searchUser(email: email)
                }) {
                    HStack {
                        if viewModel.isLoading {
                            ProgressView()
                                .scaleEffect(0.8)
                        }
                        Text("Search User")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
                .disabled(email.isEmpty || viewModel.isLoading)
                .padding(.horizontal)
                
                // User found section
                if let foundUser = viewModel.foundUser {
                    VStack {
                        HStack {
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(width: 50, height: 50)
                                .overlay(
                                    Image(systemName: "person.fill")
                                        .foregroundColor(.gray)
                                )
                            
                            VStack(alignment: .leading) {
                                Text(foundUser.displayName)
                                    .font(.headline)
                                Text(foundUser.email)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                        }
                        .padding()
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(8)
                        
                        Button(action: {
                            viewModel.sendFriendRequest(email: email)
                        }) {
                            HStack {
                                if viewModel.isLoading {
                                    ProgressView()
                                        .scaleEffect(0.8)
                                }
                                Text("Send Friend Request")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                        }
                        .disabled(viewModel.isLoading)
                    }
                    .padding(.horizontal)
                }
                
                if viewModel.userNotFound {
                    Text("No user found with that email address")
                        .foregroundColor(.red)
                        .padding(.horizontal)
                }
                
                Spacer()
            }
            .navigationTitle("Add Friend")
            .navigationBarItems(
                leading: Button("Cancel") {
                    presentationMode.wrappedValue.dismiss()
                }
            )
        }
        .alert("Error", isPresented: .constant(viewModel.errorMessage != nil)) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .alert("Success", isPresented: .constant(viewModel.requestSent)) {
            Button("OK") {
                presentationMode.wrappedValue.dismiss()
            }
        } message: {
            Text("Friend request sent successfully!")
        }
    }
}

struct AddFriendView_Previews: PreviewProvider {
    static var previews: some View {
        AddFriendView()
    }
}