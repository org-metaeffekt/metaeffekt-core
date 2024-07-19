Pod::Spec.new do |spec|
  spec.name = 'HornsAppCore'
  spec.version = '0.5.4'
  spec.homepage = 'https://github.com/Yesferal/HornsApp-KotlinMultiplatform'
  spec.source = { :git => "https://github.com/Yesferal/HornsApp-KotlinMultiplatform.git", :tag => spec.version }
  spec.authors = { "Yesferal Cueva" => "yesferal@gmail.com" }
  spec.license = { :type => "Apache-2.0", :file => "LICENSE" }
  spec.summary = 'HornsApp Kotlin Multiplatform Core Library'
  spec.static_framework = true
  spec.vendored_frameworks = "build/bin/xcframework/releaseFramework/#{spec.name}.xcframework"
  spec.libraries = "c++"
  spec.module_name = "#{spec.name}_core"
  spec.ios.deployment_target = '12.0'

  spec.prepare_command = "sh ./script/prepare_command.sh"

  spec.pod_target_xcconfig = {
      'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64'
    }
  spec.user_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64' }

end

