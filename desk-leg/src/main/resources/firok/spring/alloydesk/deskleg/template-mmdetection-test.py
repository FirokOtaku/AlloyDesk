from mmdet.apis import init_detector, inference_detector
import mmcv

config_file = 'CONFIG_FILE'
checkpoint_file = 'MODEL_FILE'

# model = init_detector(config_file, checkpoint_file, device='cuda:0')
model = init_detector(config_file, checkpoint_file, device='cpu')

def test(path_input, path_output ):
    result = inference_detector(model, path_input)
    model.show_result(path_input, result, out_file = path_output )


TEST_BATCH

